FROM python:3.8-slim-buster

WORKDIR /python-docker

COPY requirements.in requirements.txt
RUN pip3 install -r requirements.txt

COPY . .
EXPOSE 5500
CMD ["opentelemetry-instrument","flask", "run", "--host=0.0.0.0","--port=5500"]
