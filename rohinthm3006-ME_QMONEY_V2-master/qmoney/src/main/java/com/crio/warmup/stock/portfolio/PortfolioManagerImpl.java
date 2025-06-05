
package com.crio.warmup.stock.portfolio;

import static java.time.temporal.ChronoUnit.DAYS;
import static java.time.temporal.ChronoUnit.SECONDS;
import com.crio.warmup.stock.dto.AnnualizedReturn;
import com.crio.warmup.stock.dto.Candle;
import com.crio.warmup.stock.dto.PortfolioTrade;
import com.crio.warmup.stock.dto.TiingoCandle;
import com.crio.warmup.stock.exception.StockQuoteServiceException;
import com.crio.warmup.stock.quotes.StockQuotesService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;

public class PortfolioManagerImpl implements PortfolioManager {

  private RestTemplate restTemplate;
  private StockQuotesService stockQuotesService;

  
  protected PortfolioManagerImpl(RestTemplate restTemplate) {
    this.restTemplate = restTemplate;
  }


  public PortfolioManagerImpl(StockQuotesService stockQuotesService) {
    this.stockQuotesService = stockQuotesService;
  }

  private Comparator<AnnualizedReturn> getComparator() {
    return Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed();
  }

  

  public List<Candle> getStockQuote(String symbol, LocalDate from, LocalDate to)
      throws StockQuoteServiceException, JsonProcessingException {
    return stockQuotesService.getStockQuote(symbol, from, to);
  }



  private Double getOpeningPriceOnStartDate(List<Candle> candles) {
    return candles.get(0).getOpen();
  }

  private Double getClosingPriceOnEndDate(List<Candle> candles) {
    return candles.get(candles.size() - 1).getClose();
  }

  private AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate, PortfolioTrade trade,
      Double buyPrice, Double sellPrice) {
    double total_num_years = DAYS.between(trade.getPurchaseDate(), endDate) / 365.2422;
    double totalReturns = (sellPrice - buyPrice) / buyPrice;
    double annualized_returns = Math.pow((1.0 + totalReturns), (1.0 / total_num_years)) - 1;
    return new AnnualizedReturn(trade.getSymbol(), annualized_returns, totalReturns);
  }

  @Override
  public List<AnnualizedReturn> calculateAnnualizedReturn(
      List<PortfolioTrade> portfolioTrades, LocalDate endDate) {
      
      List<AnnualizedReturn> annualizedReturns = new ArrayList<>();
      
      for (PortfolioTrade portfolioTrade : portfolioTrades) {
          try {
              // Retrieve the purchase date for the trade
              LocalDate purchaseDate = portfolioTrade.getPurchaseDate();
  
              // Fetch stock quotes using the given symbol, purchase date, and end date
              List<Candle> candles = getStockQuote(
                  portfolioTrade.getSymbol(),
                  purchaseDate,
                  endDate
              );
  
              // Validate that the list of candles is not empty
              if (candles == null || candles.isEmpty()) {
                  throw new StockQuoteServiceException(
                      "No data available for symbol: " + portfolioTrade.getSymbol()
                  );
              }
  
              // Calculate annualized returns based on the fetched candles
              AnnualizedReturn annualizedReturn = calculateAnnualizedReturns(
                  endDate,
                  portfolioTrade,
                  getOpeningPriceOnStartDate(candles),
                  getClosingPriceOnEndDate(candles)
              );
  
              // Add the calculated annualized return to the list
              annualizedReturns.add(annualizedReturn);
  
          } catch (JsonProcessingException e) {
              // Log an error if there's an issue processing JSON data
              System.err.println(
                  "Error processing JSON for trade: " + portfolioTrade + " - " + e.getMessage()
              );
          } catch (StockQuoteServiceException e) {
              // Log an error if there's an issue fetching stock quotes
              System.err.println(
                  "Error fetching stock quotes for symbol: " + portfolioTrade.getSymbol() + " - " + e.getMessage()
              );
          }
      }
  
      // Sort the annualized returns in descending order and return the result
      return annualizedReturns.stream()
          .sorted(getComparator())
          .collect(Collectors.toList());
  }
  
    


 
  @Override
public List<AnnualizedReturn> calculateAnnualizedReturnParallel(
    List<PortfolioTrade> portfolioTrades, LocalDate endDate, int numThreads)
    throws StockQuoteServiceException {

    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    List<AnnualizedReturn> annualizedReturns = new ArrayList<>();

    // Create tasks
    List<AnnualizedReturnTask> tasks = portfolioTrades.stream()
        .map(trade -> new AnnualizedReturnTask(trade, stockQuotesService, endDate))
        .collect(Collectors.toList());

    try {
        // Submit tasks and get futures
        List<Future<AnnualizedReturn>> futures = executorService.invokeAll(tasks);

        // Process results
        for (Future<AnnualizedReturn> future : futures) {
            try {
                annualizedReturns.add(future.get());
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt(); // Preserve interrupt status
                throw new StockQuoteServiceException("Task interrupted: " + e.getMessage(), e);
            } catch (ExecutionException e) {
                throw new StockQuoteServiceException("Execution failed: " + e.getMessage(), e);
            }
        }
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new StockQuoteServiceException("Task execution interrupted: " + e.getMessage(), e);
    } finally {
        // Graceful shutdown
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    // Sort and return
    return annualizedReturns.stream()
        .sorted(getComparator())
        .collect(Collectors.toList());
}


}








  






