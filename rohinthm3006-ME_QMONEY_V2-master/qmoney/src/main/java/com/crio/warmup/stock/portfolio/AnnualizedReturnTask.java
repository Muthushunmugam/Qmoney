package com.crio.warmup.stock.portfolio;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.Callable;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import static java.time.temporal.ChronoUnit.DAYS;

public class AnnualizedReturnTask implements Callable<AnnualizedReturn> {

    private final PortfolioTrade portfolioTrade;
    private final LocalDate endDate;
    private final StockQuotesService stockQuotesService;

    public AnnualizedReturnTask(PortfolioTrade portfolioTrade, StockQuotesService stockQuotesService, LocalDate endDate) {
        this.portfolioTrade = portfolioTrade;
        this.endDate = endDate;
        this.stockQuotesService = stockQuotesService;
    }

    private AnnualizedReturn computeAnnualizedReturn(LocalDate endDate, PortfolioTrade trade,
                                                     Double buyPrice, Double sellPrice) {
        double totalNumYears = DAYS.between(trade.getPurchaseDate(), endDate) / 365.2422;
        double totalReturns = (sellPrice - buyPrice) / buyPrice;
        double annualizedReturns = Math.pow((1.0 + totalReturns), (1.0 / totalNumYears)) - 1;
        return new AnnualizedReturn(trade.getSymbol(), annualizedReturns, totalReturns);
    }

    @Override
    public AnnualizedReturn call() throws StockQuoteServiceException, JsonProcessingException {
        if (endDate.isBefore(portfolioTrade.getPurchaseDate())) {
            throw new IllegalArgumentException("End date cannot be before purchase date.");
        }

        List<Candle> candles = stockQuotesService.getStockQuote(
            portfolioTrade.getSymbol(),
            portfolioTrade.getPurchaseDate(),
            endDate
        );

        if (candles == null || candles.isEmpty()) {
            throw new StockQuoteServiceException("No data available for symbol: " + portfolioTrade.getSymbol());
        }

        return computeAnnualizedReturn(
            endDate,
            portfolioTrade,
            candles.get(0).getOpen(),
            candles.get(candles.size() - 1).getClose()
        );
    }
}
