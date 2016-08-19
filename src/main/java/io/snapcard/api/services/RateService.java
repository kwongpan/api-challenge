package io.snapcard.api.services;

import com.google.common.math.DoubleMath;
import io.snapcard.api.dtos.Rate;
import org.influxdb.dto.Point;
import org.influxdb.dto.Query;
import org.influxdb.dto.QueryResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.influxdb.InfluxDBTemplate;
import org.springframework.stereotype.Service;
import rx.Observable;
import rx.functions.FuncN;

import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Service
public class RateService {

    private static final String DB_NAME = "rate";

    @Autowired
    private InfluxDBTemplate<Point> influxDBTemplate;

    public Observable<Rate> getAverageByExchanges(Set<String> exchanges) {
        List<Observable<Rate>> observableList = new ArrayList<>();
        for (String exchange: exchanges) {
            Observable<Rate> rate = getLatestRateByExchange(exchange);
            observableList.add(rate);
        }

        return getAverageRate(observableList);
    }

    public Observable<Rate> getAverageByExchangesAndTime(Set<String> exchanges, Long time) {
        List<Observable<Rate>> observableList = new ArrayList<>();
        for (String exchange: exchanges) {
            Observable<Rate> rate = getRateByExchangeAndTime(exchange, time);
            observableList.add(rate);
        }

        return getAverageRate(observableList);
    }

    public Observable<Rate> getRateByExchangeAndTime(String exchange, Long time) {
        Query query = new Query("SELECT bid, ask, last FROM exchange_rate WHERE exchange='" + exchange + "' AND time=" + time +  " LIMIT 1", DB_NAME);

        QueryResult queryResult = influxDBTemplate.query(query);
        QueryResult.Result result = queryResult.getResults().get(0);
        if (result.getSeries() == null) {
            return Observable.empty();
        }
        Rate rate = seriesToRate(result.getSeries().get(0));
        return Observable.just(rate);
    }

    public Observable<Rate> getLatestRateByExchange(String exchange) {
        Query query = new Query("SELECT bid, ask, last FROM exchange_rate WHERE exchange='" + exchange + "' ORDER BY time desc LIMIT 1", DB_NAME);

        QueryResult queryResult = influxDBTemplate.query(query);
        QueryResult.Result result = queryResult.getResults().get(0);
        if (result.getSeries() == null) {
            return Observable.empty();
        }
        Rate rate = seriesToRate(result.getSeries().get(0));
        return Observable.just(rate);
    }

    private Rate seriesToRate(QueryResult.Series series) {
        Map<String, Object> map = new HashMap<>();
        List<String> columnNames = series.getColumns();
        List<Object> values = series.getValues().get(0);

        for (int i = 0; i < columnNames.size(); i++) {
            map.put(columnNames.get(i), values.get(i));
        }

        return Rate.builder().bid(new BigDecimal((String) map.get("bid")))
                .ask(new BigDecimal((String) map.get("ask")))
                .last(new BigDecimal((String) map.get("last")))
                .build();
    }

    public Point writePoint(String exchange, BigDecimal bid, BigDecimal ask, BigDecimal last, Date tickerTimeStamp,
                            Date normalizedTimeStamp) {
        final Point p = Point.measurement("exchange_rate")
                .time(normalizedTimeStamp.getTime(), TimeUnit.MILLISECONDS)
                .tag("exchange", exchange)
                .addField("bid", bid.toString())
                .addField("ask", ask.toString())
                .addField("last", last.toString())
                .addField("ticker_ts", tickerTimeStamp.getTime())
                .build();
        influxDBTemplate.write(p);
        return p;
    }

    private Double getAverage(List<BigDecimal> input) {
        double average = DoubleMath.mean(input);
        return Math.round((average * 100.0)) / 100.0;
    }

    private Observable<Rate> getAverageRate(List<Observable<Rate>> input) {
        return Observable.zip(input, new FuncN<Rate>() {
            @Override
            public Rate call(Object... args) {
                List<BigDecimal> bidList = new ArrayList<>();
                List<BigDecimal> askList = new ArrayList<>();
                List<BigDecimal> lastList = new ArrayList<>();
                for(Object obj: args) {
                    Rate r = (Rate) obj;
                    bidList.add(r.getBid());
                    askList.add(r.getAsk());
                    lastList.add(r.getLast());
                }

                return Rate.builder().bid(new BigDecimal(getAverage(bidList)))
                        .ask(new BigDecimal(getAverage(askList)))
                        .last(new BigDecimal(getAverage(lastList)))
                        .build();
            }
        });
    }

}
