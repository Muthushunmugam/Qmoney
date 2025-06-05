package com.crio.warmup.stock;

import com.crio.warmup.stock.dto.*;
import com.crio.warmup.stock.log.UncaughtExceptionHandler;
import com.crio.warmup.stock.portfolio.PortfolioManager;
import com.crio.warmup.stock.portfolio.PortfolioManagerFactory;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;
import java.util.stream.Stream;
import java.util.stream.Stream;
import org.apache.logging.log4j.ThreadContext;
import org.springframework.web.client.RestTemplate;


public class PortfolioManagerApplication {


    public static List<String> mainReadFile(String[] args) throws IOException, URISyntaxException {
        // Check if no arguments are passed, if so, use default filename
        if (args.length == 0) {
            System.out.println("No arguments provided. Using default 'trades.json' for testing.");
            args = new String[] {"trades.json"}; // Default to "trades.json"
        }

        // Resolve the file path
        File file = resolveFileFromResources(args[0]);

        // Check if the file exists and is not empty
        if (!file.exists() || file.length() == 0) {
            // Return an empty list if the file is missing or empty
            return Arrays.asList();
        }

        // Parse the JSON file
        ObjectMapper objectMapper = getObjectMapper();
        PortfolioTrade[] tradesArray = objectMapper.readValue(file, PortfolioTrade[].class);

        // Log the parsed data to check if the deserialization is correct
        System.out.println("Parsed Trades: " + Arrays.toString(tradesArray));

        if (tradesArray.length == 0) {
            // If no trades, return an empty list
            return Arrays.asList();
        }

        // Convert the array to a list and extract symbols
        List<PortfolioTrade> trades = Arrays.asList(tradesArray);
        return trades.stream().map(PortfolioTrade::getSymbol).collect(Collectors.toList());
    }



    private static void printJsonObject(Object object) throws IOException {
        Logger logger = Logger.getLogger(PortfolioManagerApplication.class.getCanonicalName());
        ObjectMapper mapper = getObjectMapper();
        logger.info(mapper.writeValueAsString(object));
    }

    private static File resolveFileFromResources(String filename) throws URISyntaxException {
        return Paths
                .get(Thread.currentThread().getContextClassLoader().getResource(filename).toURI())
                .toFile();
    }

    private static ObjectMapper getObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        return objectMapper;
    }

    public static List<String> debugOutputs() {
        
        String valueOfArgument0 = "trades.json";
        String resultOfResolveFilePathArgs0 =
                "/home/crio-user/workspace/rohinthm3006-ME_QMONEY_V2/qmoney/src/main/resources/trades.json";
        String toStringOfObjectMapper = "com.fasterxml.jackson.databind.ObjectMapper@<hashcode>";
        String functionNameFromTestFileInStackTrace ="mainReadFile";
        String lineNumberFromTestFileInStackTrace = "";

     
        return Arrays.asList(valueOfArgument0, resultOfResolveFilePathArgs0, toStringOfObjectMapper,
                functionNameFromTestFileInStackTrace, lineNumberFromTestFileInStackTrace);
    }



    public static List<String> mainReadQuotes(String[] args) throws IOException, URISyntaxException {
        if (args == null || args.length < 2) {
            throw new IllegalArgumentException("Filename and end date are required as arguments");
        }
    
        String filename = args[0];
        String endDate = args[1];
        ObjectMapper objectMapper = getObjectMapper();
        List<PortfolioTrade> trades = Arrays.asList(
                objectMapper.readValue(resolveFileFromResources(filename), PortfolioTrade[].class));
    
        // Handle empty JSON file
        if (trades.isEmpty()) {
            return Collections.emptyList();
        }
    
        LocalDate endLocalDate = LocalDate.parse(endDate);
    
        // Validate purchase dates
        for (PortfolioTrade trade : trades) {
            if (trade.getPurchaseDate().isAfter(endLocalDate)) {
                throw new RuntimeException(
                        "Purchase date is after the end date for symbol: " + trade.getSymbol());
            }
        }
    
        // Fetch sorted stock symbols based on closing prices
        List<TotalReturnsDto> sortedByValue = mainReadQuotesHelper(endDate, trades);
        sortedByValue.sort(Comparator.comparing(TotalReturnsDto::getClosingPrice));
    
        // Extract stock symbols
        List<String> stocks = new ArrayList<>();
        for (TotalReturnsDto trd : sortedByValue) {
            stocks.add(trd.getSymbol());
        }
    
        return stocks;
    }

    public static List<TotalReturnsDto> mainReadQuotesHelper(String endDate, List<PortfolioTrade> trades) {
        RestTemplate restTemplate = new RestTemplate();
        List<TotalReturnsDto> results = new ArrayList<>();
        String token = "7ccd7b1d5677b80bb785a9977920218cd4caf22b";
    
        for (PortfolioTrade trade : trades) {
            try {
                String url = prepareUrl(trade, LocalDate.parse(endDate), token);
                TiingoCandle[] candles = restTemplate.getForObject(url, TiingoCandle[].class);
    
                // Check for missing or invalid stock data
                if (candles == null || candles.length == 0) {
                    throw new RuntimeException("No data available for symbol: " + trade.getSymbol());
                }
    
                // Sort by date to ensure we get the most recent closing price
                Arrays.sort(candles, Comparator.comparing(TiingoCandle::getDate));
                TiingoCandle lastCandle = candles[candles.length - 1];
                results.add(new TotalReturnsDto(trade.getSymbol(), lastCandle.getClose()));
            } catch (Exception e) {
                throw new RuntimeException("Error processing symbol: " + trade.getSymbol(), e);
            }
        }
        return results;
    }
    
    
    



    public static List<PortfolioTrade> readTradesFromJson(String filename)
            throws IOException, URISyntaxException {

        ObjectMapper objectMapper = getObjectMapper();

        return Arrays.asList(
                objectMapper.readValue(resolveFileFromResources(filename), PortfolioTrade[].class));
    }


    public static String prepareUrl(PortfolioTrade trade, LocalDate endDate, String token) {
        return String.format(
                "https://api.tiingo.com/tiingo/daily/%s/prices?startDate=%s&endDate=%s&token=%s",
                trade.getSymbol(), trade.getPurchaseDate(), endDate, token);
    }

    // TODO: CRIO_TASK_MODULE_CALCULATIONS
    // Now that you have the list of PortfolioTrade and their data, calculate annualized returns
    // for the stocks provided in the Json.
    // Use the function you just wrote #calculateAnnualizedReturns.
    // Return the list of AnnualizedReturns sorted by annualizedReturns in descending order.

    // Note:
    // 1. You may need to copy relevant code from #mainReadQuotes to parse the Json.
    // 2. Remember to get the latest quotes from Tiingo API.
    // Get opening price on the start date

    static Double getOpeningPriceOnStartDate(List<Candle> candles) {
        return candles.stream().min(Comparator.comparing(Candle::getDate)).map(Candle::getOpen)
                .orElseThrow(() -> new RuntimeException("No opening price found"));
    }

    public static Double getClosingPriceOnEndDate(List<Candle> candles) {
        return candles.stream().max(Comparator.comparing(Candle::getDate)).map(Candle::getClose)
                .orElseThrow(() -> new RuntimeException("No closing price found"));
    }

    public static AnnualizedReturn calculateAnnualizedReturns(LocalDate endDate,
            PortfolioTrade trade, Double buyPrice, Double sellPrice) {

        double totalReturn = (sellPrice - buyPrice) / buyPrice;
        long daysBetween = ChronoUnit.DAYS.between(trade.getPurchaseDate(), endDate);
        double annualizedReturn = Math.pow(1 + totalReturn, 365.0 / daysBetween) - 1;

        return new AnnualizedReturn(trade.getSymbol(), annualizedReturn, totalReturn);
    }

    public static List<Candle> fetchCandles(PortfolioTrade trade, LocalDate endDate, String token) {
        RestTemplate restTemplate = new RestTemplate();
        String url = prepareUrl(trade, endDate, token);
    
        try {
            TiingoCandle[] candles = restTemplate.getForObject(url, TiingoCandle[].class);
            if (candles == null || candles.length == 0) {
                throw new RuntimeException("No data found for stock: " + trade.getSymbol());
            }
            return Arrays.asList(candles);
        } catch (Exception e) {
            throw new RuntimeException("Error fetching data for stock: " + trade.getSymbol(), e);
        }
    }
    

    public static List<AnnualizedReturn> mainCalculateSingleReturn(String[] args)
        throws IOException, URISyntaxException {

    List<PortfolioTrade> trades = readTradesFromJson(args[0]);
    LocalDate endDate = LocalDate.parse(args[1]);
    List<AnnualizedReturn> annret = new ArrayList<>();

    for (PortfolioTrade tr : trades) {
        try {
            String token = PortfolioManagerApplication.getToken();
            List<Candle> candles = fetchCandles(tr, endDate, token);

            if (candles.isEmpty()) {
                throw new RuntimeException("No candle data found for " + tr.getSymbol());
            }

            double buyPrice = candles.get(0).getOpen();
            double sellPrice = candles.get(candles.size() - 1).getClose();

            AnnualizedReturn ar = calculateAnnualizedReturns(endDate, tr, buyPrice, sellPrice);
            annret.add(ar);
        } catch (Exception e) {
            throw new RuntimeException("Error processing trade for symbol: " + tr.getSymbol(), e);
        }
    }

    annret.sort(Comparator.comparing(AnnualizedReturn::getAnnualizedReturn).reversed());
    return annret;
}



    public static class AnnualReturnComparator implements Comparator<AnnualizedReturn> {
        public int compare(AnnualizedReturn a1, AnnualizedReturn a2) {
            if (a1.getAnnualizedReturn() < a2.getAnnualizedReturn()) {
                return 1;
            } else if (a1.getAnnualizedReturn() > a2.getAnnualizedReturn()) {
                return -1;
            } else {
                return 0;
            }
        }
    }


    public static String getToken() {
        String token = "7ccd7b1d5677b80bb785a9977920218cd4caf22b";
        return token;
    }
	
	// TODO: CRIO_TASK_MODULE_REFACTOR
  //  Once you are done with the implementation inside PortfolioManagerImpl and
  //  PortfolioManagerFactory, create PortfolioManager using PortfolioManagerFactory.
  //  Refer to the code from previous modules to get the List<PortfolioTrades> and endDate, and
  //  call the newly implemented method in PortfolioManager to calculate the annualized returns.

  // Note:
  // Remember to confirm that you are getting same results for annualized returns as in Module 3.

  private static String readFileAsString(String fileName) throws IOException, URISyntaxException {
    return new String(Files.readAllBytes(resolveFileFromResources(fileName).toPath()), "UTF-8");
  }

  public static List<AnnualizedReturn> mainCalculateReturnsAfterRefactor(String[] args)
      throws Exception {
       String file = args[0];
       LocalDate endDate = LocalDate.parse(args[1]);
       String contents = readFileAsString(file);
       ObjectMapper objectMapper = getObjectMapper();
       //
       PortfolioManager portfolioManager = PortfolioManagerFactory.getPortfolioManager(new RestTemplate());
       List<PortfolioTrade> portfolioTrades = objectMapper.readValue(contents,new TypeReference<List<PortfolioTrade>>() {});

       return portfolioManager.calculateAnnualizedReturn(portfolioTrades, endDate);
  }



    public static void main(String[] args) throws Exception {
        Thread.setDefaultUncaughtExceptionHandler(new UncaughtExceptionHandler());
        ThreadContext.put("runId", UUID.randomUUID().toString());

        // Provide default arguments if none are provided
        if (args.length < 2) {
            System.out.println(
                    "No arguments provided. Using default 'trades.json' and '2019-01-02' for testing.");
            args = new String[] {
                    "/home/crio-user/workspace/rohinthm3006-ME_QMONEY_V2/qmoney/src/main/resources/trades.json",
                    "2019-01-02"}; // Default values
        }

        try {
            printJsonObject(mainReadQuotes(args));
        } catch (Exception e) {
            System.out.println(
                    "An error occurred while processing the trades or fetching the stock data.");
            e.printStackTrace();
        }
    }


}






















  



