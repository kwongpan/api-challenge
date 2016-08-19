Install InfluxDB on local machine
---------------------------------

https://docs.influxdata.com/influxdb/v0.13/introduction/installation/

Then create the database
------------------------

curl 'http://localhost:8086/query' --data-urlencode "q=CREATE DATABASE rate"

Run the project
---------------
You don't need to install anything. All you need is you have java installed. Then run

./gradlew bootRun

Let it runs for 30 sec to a minute so that it can save some data to the db and then you are ready to test.

Endpoints
---------
http://localhost:8080/rates/average

http://localhost:8080/rates/historical/:time
e.g. http://localhost:8080/rates/historical/2016-08-19T15:12:45Z

http://localhost:8080/rates/historical/:time/:exchange
e.g. http://localhost:8080/rates/historical/2016-08-19T15:12:45Z/okcoinus


:time - ISO datetime format yyyy-MM-dd'T'HH:mm:ss.SSSZ
e.g.
2016-08-19T15:12:45Z
2016-08-19T15:12:42Z -> would round to 2016-08-19T15:12:40Z

:exchange - current available options are:
okcoinus
bitfinex

Note
----

Create database

curl 'http://localhost:8086/query' --data-urlencode "q=CREATE DATABASE rate"

Drop database

curl 'http://localhost:8086/query' --data-urlencode "q=DROP DATABASE rate"

API Source Endpoints
--------------------

https://www.okcoin.com/api/v1/ticker.do?symbol=btc_usd

https://api.bitfinex.com/v1/pubticker/btcusd