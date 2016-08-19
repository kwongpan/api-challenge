package io.snapcard.api.jobs;

import io.snapcard.api.services.RateService;
import org.knowm.xchange.Exchange;
import org.knowm.xchange.ExchangeFactory;
import org.knowm.xchange.ExchangeSpecification;
import org.knowm.xchange.dto.marketdata.Ticker;
import org.knowm.xchange.okcoin.OkCoinExchange;
import org.knowm.xchange.service.polling.marketdata.PollingMarketDataService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Date;

import static org.knowm.xchange.currency.CurrencyPair.BTC_USD;

@Component
public class OkCoinUSGetRateTask {

    private static final String EXCHANGE = "okcoinus";

    private static Exchange exchangeInstance = null;

    private OkCoinUSGetRateTask() {}

    @Autowired
    private RateService rateService;

    public static Exchange getExchangeInstance() {
        if (exchangeInstance == null) {
            synchronized (OkCoinUSGetRateTask.class) {
                ExchangeSpecification exSpec = new ExchangeSpecification(OkCoinExchange.class);
                exSpec.setExchangeSpecificParametersItem("Use_Intl", true);
                exchangeInstance = ExchangeFactory.INSTANCE.createExchange(exSpec);
                exchangeInstance.applySpecification(exSpec);
            }
        }
        return exchangeInstance;
    }

    @Scheduled(cron = "*/5 * * * * *")
    public void getRate() {

        try {
            PollingMarketDataService marketDataService = getExchangeInstance().getPollingMarketDataService();
            Ticker ticker = marketDataService.getTicker(BTC_USD);
//            System.out.println("okcoin: " + ticker + " " + ticker.getTimestamp());

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
