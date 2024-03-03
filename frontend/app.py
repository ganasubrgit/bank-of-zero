
"""Web service for frontend
"""

# Module imports
import concurrent.futures
import datetime
import json
import logging
import os
import socket
from decimal import Decimal, DecimalException
from time import sleep
from opentelemetry import trace
import traced_thread_pool_executor
from opentelemetry.instrumentation.flask import FlaskInstrumentor
from opentelemetry.instrumentation.requests import RequestsInstrumentor
from prometheus_flask_exporter import PrometheusMetrics


import requests
from requests.exceptions import HTTPError, RequestException
import jwt
from flask import Flask, abort, jsonify, make_response, redirect, \
    render_template, request, url_for


# Local imports
from api_call import ApiCall, ApiRequest

import logging_loki
logging_loki.emitter.LokiEmitter.level_tag = "level"
# assign to a variable named handler 
handler = logging_loki.LokiHandler(
   url="http://loki:3100/loki/api/v1/push",
   version="1",
)
# create a new logger instance, name it whatever you want
logger = logging.getLogger("loki-logger")

logger.addHandler(handler)

logger.setLevel("INFO")
# Local constants
BALANCE_NAME = "balance"
CONTACTS_NAME = "contacts"
TRANSACTION_LIST_NAME = "transaction_list"


# pylint: disable-msg=too-many-locals
# pylint: disable-msg=too-many-branches
def create_app():
    """Flask application factory to create instances
    of the Frontend Flask App
    """
    app = Flask(__name__)
    FlaskInstrumentor().instrument_app(app)
    RequestsInstrumentor().instrument()
    tracer = trace.get_tracer(__name__)
    metrics = PrometheusMetrics(app)

    # Disabling unused-variable for lines with route decorated functions
    # as pylint thinks they are unused
    # pylint: disable=unused-variable
    @app.route('/version', methods=['GET'])
    def version():
        """
        Service version endpoint
        """
        return os.environ.get('VERSION'), 200

    @app.route('/ready', methods=['GET'])
    def readiness():
        """
        Readiness probe
        """
        return 'ok', 200

    @app.route('/whereami', methods=['GET'])
    def whereami():
        """
        Returns the cluster name + zone name where this Pod is running.

        """
        return "Cluster: " + 'localhost' , 200

    @app.route("/")
    def root():
        """
        Renders home page or login page, depending on authentication status.
        """
        with tracer.start_as_current_span("frontend-root"):
            token = request.cookies.get("altimetrik")
            if not verify_token(token):
                return login_page()
        return home()

    @app.route("/home")
    def home():
        """
        Renders home page. Redirects to /login if token is not valid
        """
        with tracer.start_as_current_span("frontend-root"):
            token = request.cookies.get("altimetrik")
            if not verify_token(token):
            # user isn't authenticated
                logger.debug('User isn\'t authenticated. Redirecting to login page.')
                return redirect(url_for('login_page',
                                    _external=True,
                                    _scheme=app.config['SCHEME']))
            token_data = decode_token(token)
            display_name = token_data['name']
            username = token_data['name']
            account_id = token_data['account']

            hed = {'Authorization': 'Bearer ' + token}

            api_calls = [
            # get balance
            ApiCall(display_name=BALANCE_NAME,
                    api_request=ApiRequest(url='http://deposit-service:1980/account/fetch-balance?name='+username,
                                           headers=hed,
                                           timeout=60),
                    logger=logger),
            # get history
            ApiCall(display_name=TRANSACTION_LIST_NAME,
                    api_request=ApiRequest(url='http://deposit-service:1980/account/get-transactions-by-name?name='+username+'&recipient='+username,
                                           headers=hed,
                                           timeout=60),
                    logger=logger),
            # get contacts
            ApiCall(display_name=CONTACTS_NAME,
                    api_request=ApiRequest(url='http://deposit-service:1980/account/get-all',
                                           headers=hed,
                                           timeout=60),
                    logger=logger)
            ]

            api_response = {BALANCE_NAME: None,
                            TRANSACTION_LIST_NAME: None,
                            CONTACTS_NAME: []}

            with traced_thread_pool_executor.TracedThreadPoolExecutor(tracer, max_workers=3) as executor:
                futures = []

                future_to_api_call = {
                    executor.submit(api_call.make_call):
                        api_call for api_call in api_calls
                }

                for future in concurrent.futures.as_completed(future_to_api_call):
                    if future.result():
                        api_call = future_to_api_call[future]
                        api_response[api_call.display_name] = future.result().json()

            _populate_contact_labels(username,
                                 api_response[TRANSACTION_LIST_NAME],
                                 api_response[CONTACTS_NAME])


        return render_template('index.html',
                               account_id=account_id,
                               username=username,
                               balance=api_response[BALANCE_NAME],
                               bank_name=os.getenv('BANK_NAME', 'Bank of Altimetrik'),
                               cluster_name='localhost',
                               contacts=api_response[CONTACTS_NAME],
                               cymbal_logo=os.getenv('CYMBAL_LOGO', 'false'),
                               history=api_response[TRANSACTION_LIST_NAME],
                               message=request.args.get('msg', None),
                               name=display_name,
                               platform=platform,
                               platform_display_name=platform_display_name)

    def _populate_contact_labels(username, transactions, contacts):
        logger.debug('Populating contact labels.')
        if username is None or transactions is None or contacts is None:
            return

        # Map contact accounts to their labels. If no label found, default to None.
        contact_map = {c['name']: c.get('name') for c in contacts}

        # Populate the 'accountLabel' field. If no match found, default to None.
        for trans in transactions:
            if trans['recipient'] == username:
                trans['accountLabel'] = contact_map.get(trans['recipient'])
            elif trans['name'] == username:
                trans['accountLabel'] = contact_map.get(trans['recipient'])

    @app.route('/payment', methods=['POST'])
    def payment():
        """
        Submits payment request to ledgerwriter service

        Fails if:
        - token is not valid
        - basic validation checks fail
        - response code from ledgerwriter is not 201
        """
        with tracer.start_as_current_span("frontend-payment"):
            token = request.cookies.get("altimetrik")
            if not verify_token(token):
                # user isn't authenticated
                logger.error('Error submitting payment: user is not authenticated.')
                return abort(401)
            try:
                account_id = decode_token(token)['name']
                recipient = request.form['account_num']
                if recipient == 'add':
                    recipient = request.form['contact_account_num']
                    label = request.form.get('contact_label', None)
                user_input = request.form['amount']
                payment_amount = int(user_input)
                transaction_data = {"fromAccountNum": account_id,
                                "toAccountNum": recipient,
                                "amount": payment_amount,
                                "uuid": request.form['uuid']}
                resp = requests.post(url='http://deposit-service:1980/account/transfer?from='+account_id+'&to='+recipient+'&balance='+str(payment_amount),
                             data=jsonify(transaction_data).data,
                             timeout=app.config['BACKEND_TIMEOUT'])
                try:
                    resp.raise_for_status()  # Raise on HTTP Status code 4XX or 5XX
                except requests.exceptions.HTTPError as http_request_err:
                    raise UserWarning(resp.text) from http_request_err
            # Short delay to allow the transaction to propagate to balancereader
            # and transaction-history
                sleep(0.25)
                logger.info('Payment initiated successfully.')
                return redirect(code=303,
                            location=url_for('home',
                                             msg='Payment successful',
                                             _external=True,
                                             _scheme=app.config['SCHEME']))

            except requests.exceptions.RequestException as err:
                logger.error('Error submitting payment: %s', str(err))
            except UserWarning as warn:
                logger.error('Error submitting payment: %s', str(warn))
                msg = 'Payment failed: {}'.format(str(warn))
                return redirect(url_for('home',
                                    msg=msg,
                                    _external=True,
                                    _scheme=app.config['SCHEME']))
            except (ValueError, DecimalException) as num_err:
                logger.error('Error submitting payment: %s', str(num_err))
                msg = 'Payment failed: {} is not a valid number'.format(user_input)

        return redirect(url_for('home',
                                msg='Payment failed',
                                _external=True,
                                _scheme=app.config['SCHEME']))

    @app.route('/deposit', methods=['POST'])
    def deposit():
        """
        Submits deposit request to ledgerwriter service

        Fails if:
        - token is not valid
        - routing number == local routing number
        - response code from ledgerwriter is not 201
        """
        with tracer.start_as_current_span("frontend-deposit"):
            token = request.cookies.get("altimetrik")
            if not verify_token(token):
                # user isn't authenticated
                logger.error('Error submitting deposit: user is not authenticated.')
                return abort(401)
            try:
                # get account id from token
                account_id = decode_token(token)['name']
                if request.form['account'] == 'add':
                    external_account_num = request.form['external_account_num']
                    external_routing_num = request.form['external_routing_num']
                
                    external_label = request.form.get('external_label', None)

                resp = requests.post(url='http://deposit-service:1980/account/deposit?name='+account_id+'&balance='+str(Decimal(request.form['amount'])),
                             timeout=app.config['BACKEND_TIMEOUT'])
                try:
                    resp.raise_for_status()  # Raise on HTTP Status code 4XX or 5XX
                except requests.exceptions.HTTPError as http_request_err:
                    raise UserWarning(resp.text) from http_request_err
                # Short delay to allow the transaction to propagate to balancereader
                # and transaction-history
                sleep(0.25)
                logger.info('Deposit submitted successfully.')
                return redirect(code=303,
                            location=url_for('home',
                                             msg='Deposit successful',
                                             _external=True,
                                             _scheme=app.config['SCHEME']))

            except requests.exceptions.RequestException as err:
                logger.error('Error submitting deposit: %s', str(err))
            except UserWarning as warn:
                logger.error('Error submitting deposit: %s', str(warn))
                msg = 'Deposit failed: {}'.format(str(warn))
                return redirect(url_for('home',
                                    msg=msg,
                                    _external=True,
                                    _scheme=app.config['SCHEME']))

        return redirect(url_for('home',
                                msg='Deposit failed',
                                _external=True,
                                _scheme=app.config['SCHEME']))

    
    @app.route("/login", methods=['GET'])
    def login_page():
        """
        Renders login page. Redirects to /home if user already has a valid token.
        If this is an oauth flow, then redirect to a consent form.
        """
        with tracer.start_as_current_span("frontend-login-internal"):
            token = request.cookies.get("altimetrik")
            response_type = request.args.get('response_type')
            client_id = request.args.get('client_id')
            app_name = request.args.get('app_name')
            redirect_uri = request.args.get('redirect_uri')
            state = request.args.get('state')
            if ('REGISTERED_OAUTH_CLIENT_ID' in os.environ and
                'ALLOWED_OAUTH_REDIRECT_URI' in os.environ and
                response_type == 'code'):
                logger.debug('Login with response_type=code')
                if client_id != os.environ['REGISTERED_OAUTH_CLIENT_ID']:
                    return redirect(url_for('login',
                                        msg='Error: Invalid client_id',
                                        _external=True,
                                        _scheme=app.config['SCHEME']))
                if redirect_uri != os.environ['ALLOWED_OAUTH_REDIRECT_URI']:
                    return redirect(url_for('login',
                                        msg='Error: Invalid redirect_uri',
                                        _external=True,
                                        _scheme=app.config['SCHEME']))
                if verify_token(token):
                    logger.debug('User already authenticated. Redirecting to /consent')
                    return make_response(redirect(url_for('consent',
                                                      state=state,
                                                      redirect_uri=redirect_uri,
                                                      app_name=app_name,
                                                      _external=True,
                                                      _scheme=app.config['SCHEME'])))
            else:
                if verify_token(token):
                    # already authenticated
                    logger.debug('User already authenticated. Redirecting to /home')
                    return redirect(url_for('home',
                                        _external=True,
                                        _scheme=app.config['SCHEME']))

        return render_template('login.html',
                               app_name=app_name,
                               bank_name=os.getenv('BANK_NAME', 'Bank of Zero'),
                               cluster_name='localhost',
                               cymbal_logo=os.getenv('CYMBAL_LOGO', 'false'),
                               default_password=os.getenv('DEFAULT_PASSWORD', ''),
                               default_user=os.getenv('DEFAULT_USERNAME', ''),
                               message=request.args.get('msg', None),
                               platform=platform,
                               platform_display_name=platform_display_name,
                               redirect_uri=redirect_uri,
                               response_type=response_type,
                               state=state)

    @app.route('/login', methods=['POST'])
    def login():
        """
        Submits login request to userservice and saves resulting token

        Fails if userservice does not accept input username and password
        """
        with tracer.start_as_current_span("frontend-login-external"):
            return _login_helper(request.form['username'],
                             request.form['password'],
                             request.args)

    def _login_helper(username, password, request_args):
        try:
            logger.debug('Logging in.')
            req = requests.post(url="http://account-management:8180/api/auth/signin",
                               data=json.dumps({"username": username, "password": password}),
                               headers={"Content-Type":"application/json"},
                               timeout=app.config['BACKEND_TIMEOUT']*2)
            req.raise_for_status()  # Raise on HTTP Status code 4XX or 5XX

            # login success
            token = req.json()['token']
            key = req.json()['key']
            claims = decode_token(token)

            max_age = claims['exp'] - claims['iat']
 
            if ('response_type' in request_args and
                'state' in request_args and
                'redirect_uri' in request_args and
                    request_args['response_type'] == 'code'):
                resp = make_response(redirect(url_for('consent',
                                                      state=request_args['state'],
                                                      redirect_uri=request_args['redirect_uri'],
                                                      app_name=request_args['app_name'],
                                                      _external=True,
                                                      _scheme=app.config['SCHEME'])))
                
            else:
                resp = make_response(redirect(url_for('home',
                                                      _external=True,
                                                      _scheme='http')))
            resp.set_cookie("altimetrik", token, max_age)
            logger.info('Successfully logged in.')
            return resp
        except (RequestException, HTTPError) as err:
            logger.error('Error logging in: %s', str(err))
        return redirect(url_for('login',
                                msg='Login Failed',
                                _external=True,
                                _scheme=app.config['SCHEME']))

    @app.route("/consent", methods=['GET'])
    def consent_page():
        """Renders consent page.

        Retrieves auth code if the user has
        already logged in and consented.
        """
        redirect_uri = request.args.get('redirect_uri')
        state = request.args.get('state')
        app_name = request.args.get('app_name')
        token = request.cookies.get("altimetrik")
        consented = request.cookies.get("altimetrik")
        if verify_token(token):
            if consented == "true":
                logger.debug('User consent already granted.')
                resp = _auth_callback_helper(state, redirect_uri, token)
                return resp

            return render_template('consent.html',
                                   app_name=app_name,
                                   bank_name=os.getenv('BANK_NAME', 'Bank of Zero'),
                                   cluster_name='localhost',
                                   cymbal_logo=os.getenv('CYMBAL_LOGO', 'false'),
                                   platform=platform,
                                   platform_display_name=platform_display_name,

                                   redirect_uri=redirect_uri,
                                   state=state)

        return make_response(redirect(url_for('login',
                                              response_type="code",
                                              state=state,
                                              redirect_uri=redirect_uri,
                                              app_name=app_name,
                                              _external=True,
                                              _scheme=app.config['SCHEME'])))

    @app.route('/consent', methods=['POST'])
    def consent():
        """
        Check consent, write cookie if yes, and redirect accordingly
        """
        consent = request.args['consent']
        state = request.args['state']
        redirect_uri = request.args['redirect_uri']
        token = request.cookies.get("altimetrik")

        logger.debug('Checking consent. consent: %s', consent)

        if consent == "true":
            logger.info('User consent granted.')
            resp = _auth_callback_helper(state, redirect_uri, token)
            resp.set_cookie("altimetrtik", 'true')
        else:
            logger.info('User consent denied.')
            resp = make_response(redirect(redirect_uri + '#error=access_denied', 302))
        return resp

    def _auth_callback_helper(state, redirect_uri, token):
        try:
            logger.debug('Retrieving authorization code.')
            callback_response = requests.post(url=redirect_uri,
                                              data={'state': state, 'id_token': token},
                                              timeout=app.config['BACKEND_TIMEOUT'],
                                              allow_redirects=False)
            if callback_response.status_code == requests.codes.found:
                logger.info('Successfully retrieved auth code.')
                location = callback_response.headers['Location']
                return make_response(redirect(location, 302))

            logger.error('Unexpected response status: %s', callback_response.status_code)
            return make_response(redirect(redirect_uri + '#error=server_error', 302))
        except requests.exceptions.RequestException as err:
            logger.error('Error retrieving auth code: %s', str(err))
        return make_response(redirect(redirect_uri + '#error=server_error', 302))

    @app.route("/signup", methods=['GET'])
    def signup_page():
        """
        Renders signup page. Redirects to /login if token is not valid
        """
        with tracer.start_as_current_span("frontend-signup-internal"):
            token = request.cookies.get(app.config['TOKEN_NAME'])
            if verify_token(token):
            # already authenticated
                logger.debug('User already authenticated. Redirecting to /home')
                return redirect(url_for('home',
                                    _external=True,
                                    _scheme=app.config['SCHEME']))
        return render_template('signup.html',
                               bank_name=os.getenv('BANK_NAME', 'Bank of Zero'),
                               cluster_name='localhost',
                               cymbal_logo=os.getenv('CYMBAL_LOGO', 'false'),
                               platform=platform,
                               platform_display_name=platform_display_name)

    @app.route("/signup", methods=['POST'])
    def signup():
        """
        Submits signup request to userservice

        Fails if userservice does not accept input form data
        """
        with tracer.start_as_current_span("frontend-signup-external"):
            try:
            # create user
                logger.debug('Creating new user.')
                resp = requests.post(url='http://account-management:8180/api/auth/signup',
                                 data=json.dumps(request.form.to_dict()),
                                 headers={"Content-Type":"application/json"},
                                 timeout=app.config['BACKEND_TIMEOUT'])
                if resp.status_code >= 200 and resp.status_code < 400:
                # user created. Attempt login
                    logger.info('New user created.')
                    req_data = request.form.to_dict()
                    resp = requests.post(url='http://deposit-service:1980/account/create',
                                 data='{"name":"'+req_data["username"]+'","account":"'+str(resp.json())+'"}',
                                 headers={"Content-Type":"application/json"},
                                 timeout=app.config['BACKEND_TIMEOUT'])
                    return _login_helper(request.form['username'],
                                     request.form['password'],
                                     request.args)
            except requests.exceptions.RequestException as err:
                logger.error('Error creating new user: %s', str(err))
        return redirect(url_for('login',
                                msg='Error: Account creation failed',
                                _external=True,
                                _scheme=app.config['SCHEME']))

    @app.route('/logout', methods=['POST'])
    def logout():
        """
        Logs out user by deleting token cookie and redirecting to login page
        """
        with tracer.start_as_current_span("frontend-logout"):
            logger.info('Logging out.')
            resp = make_response(redirect(url_for('login_page',
                                              _external=True,
                                              _scheme=app.config['SCHEME'])))
            resp.delete_cookie("altimetrik")
            resp.delete_cookie("altimetrik")
        return resp

    def decode_token(token):
        return jwt.decode(algorithms='RS256',
                          jwt=token,
                          options={"verify_signature": False})

    def verify_token(token):
        """
        Validates token using userservice public key
        """
        logger.debug('Verifying token.')
        if token is None:
            return False
        try:
            jwt.decode(algorithms='HS256',
                       jwt=token,
                       
                       options={"verify_signature": False})
            logger.debug('Token verified.')
            return True
        except jwt.exceptions.InvalidTokenError as err:
            logger.error('Error validating token: %s', str(err))
            return False

    # set up global variables
    
    # timeout in seconds for calls to the backend
    app.config['BACKEND_TIMEOUT'] = int(os.getenv('BACKEND_TIMEOUT', '4'))
    app.config['TOKEN_NAME'] = 'token'
    app.config['CONSENT_COOKIE'] = 'consented'
    app.config['TIMESTAMP_FORMAT'] = '%Y-%m-%dT%H:%M:%S.%f%z'
    app.config['SCHEME'] = os.environ.get('SCHEME', 'http')


    platform = os.getenv('ENV_PLATFORM', None)
    platform_display_name = "Local"
    

    return app


if __name__ == "__main__":
    # Create an instance of flask server when called directly
    FRONTEND = create_app()
    FRONTEND.run(port=5500)
