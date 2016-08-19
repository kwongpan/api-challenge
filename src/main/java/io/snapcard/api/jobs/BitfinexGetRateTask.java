package io.snapcard.api.jobs;

import io.snapcard.api.services.RateService;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.bitfinex.v1.BitfinexExchange;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.service.polling.marketdata.PollingMarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;

import static org.knowm.xchange.currency.CurrencyPair.BTC_USD;

@Component
public class BitfinexGetRateTask {

    private static final String EXCHANGE = "bitfinex";

    private Exchange exchangeInstance = ExchangeFactory.INSTANCE.createExchange(BitfinexExchange.class.getName());

    @Autowired
    private RateService rateService;

    @Scheduled(cron = "*/5 * * * * *")
    public void getRate() {
        try {
            PollingMarketDataService marketDataService = exchangeInstance.getPollingMarketDataService();
            Ticker ticker = marketDataService.getTicker(BTC_USD);
//            System.out.println("bitfinex: " + ticker + " " + ticker.getTimestamp());

            Date now = new Date();
            Date normalizedTimeStamp = new Date(now.getTime() - (now.getTime() % 5000));

            rateService.writePoint(EXCHANGE, ticker.getBid(), ticker.getAsk(), ticker.getLast(), ticker.getTimestamp(),
                    normalizedTimeStamp);

        } catch (IOException e) {
            // please log the error
            e.printStackTrace();
        }

    }

}
