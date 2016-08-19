package io.snapcard.api.controllers;

import io.snapcard.api.dtos.Rate;
import io.snapcard.api.services.RateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import rx.Single;

import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

@RestController
public class RateController {

    private static final List<String> EXCHANGE = Arrays.asList("bitfinex", "okcoinus");

    @Autowired
    private RateService rateService;

    @GetMapping("/rates/average")
    public Single<Rate> average() {
        Single<Rate> rate = rateService.getAverageByExchanges(new HashSet<>(EXCHANGE)).toSingle();
        return rate;
    }

    @GetMapping("/rates/historical/{time}")
    public Single<Rate> historicalAverage(@PathVariable("time")
                                          @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime dateTime) {
        Single<Rate> rate = rateService.getAverageByExchangesAndTime(new HashSet<>(EXCHANGE),
                getTime(dateTime)).toSingle();
        return rate;
    }

    /**
     * I can't figure out how to set different http status code at the moment because I'm using Spring 5.0 M1 which
     * supports rendering RxJava's Observable and Single out of the box without too much hack.
     *
     * @param dateTime
     * @param exchange
     * @return
     */
    @GetMapping("/rates/historical/{time}/{exchange}")
    public Single<Rate> historicalExchangeAverage(@PathVariable("time")
                                                  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) ZonedDateTime dateTime,
                                                  @PathVariable("exchange") String exchange/*, ServerHttpResponse response*/) {
        if (EXCHANGE.contains(exchange)) {
            Single<Rate> rate = rateService.getRateByExchangeAndTime(exchange, getTime(dateTime)).toSingle();
            return rate;
        } else {
            // need better way to handle 404 and 500 errors
            //response.setStatusCode(HttpStatus.NOT_FOUND);
            return Single.just(Rate.builder().build());
        }
    }

    private Long getTime(ZonedDateTime dateTime) {
        Date dt = Date.from(dateTime.toInstant());
        // round the date time to the pervious 5 second interval if the current second is not a multiple of 5
        Date normalizedTimeStamp = new Date(dt.getTime() - (dt.getTime() % 5000));
        // when using the official influxdb-java client library, we can only save data when the time is in
        // TimeUnit.MILLISECONDS. I have no idea why the time that is saved to the db is 1000000 bigger.
        // Therefore, in order for the query able to work, we have to multiply by 1000000 to match the time that we
        // saved in the db.
        Long time = normalizedTimeStamp.getTime() * 1000000;
        return time;
    }

}
