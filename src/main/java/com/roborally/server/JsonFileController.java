package com.roborally.server;  // Ensure this matches your directory structure

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.annotation.SubscribeMapping;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

// ALEX
//import static spark.Spark.*;

//import org.json.JSONArray;
/*
import java.io.File;
import java.util.Arrays;

public class GameServer {
    public static void main(String[] args) {
        port(8080);

        get("/games", (req, res) -> {
            File gamesDir = new File("resources/static/games");
            if (gamesDir.exists() && gamesDir.isDirectory()) {
                String[] games = gamesDir.list();
                if (games != null) {
                    JSONArray gamesArray = new JSONArray(Arrays.asList(games));
                    res.type("application/json");
                    return gamesArray.toString();
                }
            }
            res.status(404);
            return "Games directory not found or is not a directory";
        });
    }
}*/
//ALEX



@RestController
public class JsonFileController {


    private static final Logger logger = LoggerFactory.getLogger(JsonFileController.class);
    @Value("${app.json-file-path}")
    private String JSON_FILE_PATH;
    @Value("${app.board-json-file-path}")
    private String JSON_BOARD_FILE_PATH;
    @Value("${app.games-txt-file-path}")
    private String TXT_GAMES_PATH;
    //private static final String JSON_FILE_PATH = "src\\main\\resources\\static\\daskjbadw.json";  // Check your file path

    @Autowired
    private SimpMessagingTemplate template;

    @MessageMapping("/updateJson")
    @SendTo("/topic/json")
    public UpdateResponse updateJson(String jsonContent) {
        logger.info("Received JSON for update: " + jsonContent);
        LocalDateTime now = LocalDateTime.now(); // Capture the current time
        try {
            Path path = Paths.get(JSON_FILE_PATH);
            Files.write(path, jsonContent.getBytes());
            // Send the updated JSON content and the timestamp
            template.convertAndSend("/topic/json", jsonContent);
            return new UpdateResponse(jsonContent, now);
        } catch (IOException e) {
            logger.error("Failed to write JSON file at " + JSON_FILE_PATH + ": " + e.getMessage());
            // Send error info and the timestamp
            return new UpdateResponse("Failed to update file.", now);
        } catch (Exception e) {
            logger.error("An unexpected error occurred: " + e.getMessage());
            return new UpdateResponse("An unexpected error occurred.", now);
        }
    }

    @Value("${app.base-path}")
    private String BASE_PATH;

    /***
     * Sends the player data file to the clients
     * @param gameName the game which is sent from
     * @return the game data as a string
     * @author Uffe Clausen
     */
    @GetMapping("/games/{gameName}/playersb")
    public String getPlayersJson(@PathVariable String gameName) {
        try {
            Path path = Paths.get(BASE_PATH, gameName, "players.json");
            byte[] jsonData = Files.readAllBytes(path);
            return new String(jsonData);
        } catch (Exception e) {}
        return null;
    }

    /***
     * sends the board to the clients
     * @param gameName the game which the board get send from
     * @return the board as a string
     * @author Uffe Clausen
     */
    @GetMapping("/games/{gameName}/board")
    public String getBoardJson(@PathVariable String gameName) {
        try {
            Path path = Paths.get(BASE_PATH, gameName, "board.json");
            byte[] jsonBoardData = Files.readAllBytes(path);
            return new String(jsonBoardData);
        } catch (Exception e) {}
        return null;
    }

    /***
     * Updates the board then its sent
     * @param gameName game which board needs updating
     * @param jsonBoardData json file for the board
     * @return  the response entiry
     * @Uffe Clausen
     */
    @PutMapping("/games/{gameName}/board")
    public ResponseEntity<String> putBoardJson(@PathVariable String gameName, @RequestBody String jsonBoardData) {
        try {
            Path path = Paths.get(BASE_PATH, gameName, "board.json");
            Files.write(path, jsonBoardData.getBytes());
            return ResponseEntity.ok("JSON data.json for board in " + gameName + " has been updated successfully.");
        } catch (Exception e) {}
        return null;
    }

    /***
     * Sends the game data to the clients
     * @param gameName  the game which needs updating
     * @return  the response entity
     * @author Uffe Clausen
     */
    @GetMapping("/games/{gameName}/data")
    public ResponseEntity<String> getDataJson(@PathVariable String gameName) {
        try {
            Path path = Paths.get(BASE_PATH, gameName, "data.json");
            byte[] jsonBoardData = Files.readAllBytes(path);
            return ResponseEntity.ok(new String(jsonBoardData, StandardCharsets.UTF_8));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error accessing data for " + gameName);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred");
        }
    }


    /***
     * updates the game data then a moveis made on the client
     * @param gameName  game there the data gets updated
     * @param jsonBoardData json file which is sent to update the one on the server
     * @return  that the game has been updated
     * @author Uffe Clausen
     */
    @PutMapping("/games/{gameName}/data")
    public ResponseEntity<String> putDataJson(@PathVariable String gameName, @RequestBody String jsonBoardData) {
        try {
            Path path = Paths.get(BASE_PATH, gameName, "data.json");
            Files.writeString(path, jsonBoardData, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            return ResponseEntity.ok("JSON data.json for game " + gameName + " has been updated successfully.");
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to write data for " + gameName);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Unexpected error occurred");
        }
    }

    /***
     * Sends a list over the game folders to the client
     * @return return a list over games on the server
     * @author Anders Jensen
     */
    @GetMapping("/games")
    public List<String> getGames() {
        File directory = new File(TXT_GAMES_PATH);
        if (directory.exists() && directory.isDirectory()) {
            return Arrays.stream(Objects.requireNonNull(directory.listFiles()))
                    .filter(File::isDirectory)
                    .map(File::getName)
                    .filter(name -> name.matches("game\\d+"))
                    .collect(Collectors.toList());
        } else {
            throw new RuntimeException("Games directory not found");
        }
    }

    /***
     * Sends the player list to the clients
     * @param game what game the players are sent from
     * @return the string of players that are in txt file
     * @author Anders Jensen
     */
    @GetMapping("/games/{game}/players")
    public String getPlayers(@PathVariable String game) {
        File playersFile = new File(TXT_GAMES_PATH +"/"+ game + "/players.txt");
        if (playersFile.exists()) {
            try {
                return new String(Files.readAllBytes(playersFile.toPath()));
            } catch (IOException e) {
                throw new RuntimeException("Failed to read players.txt file", e);
            }
        } else {
            throw new RuntimeException("players.txt file not found for game: " + game);
        }
    }

    /***
     * Take post request and adds the next player to the txt file
     * @param game what game it is sent to of the folders
     * @param playerName the name added to the txt file
     * @return if the player is added or that it already exist
     * @author Anders Jensen
     */
    @PostMapping("/games/{game}/players")
    public String addPlayer(@PathVariable String game, @RequestParam String playerName) {
        File playersFile = new File(TXT_GAMES_PATH +"/"+ game + "/players.txt");
        try {
            if (!playersFile.exists()) {
                playersFile.getParentFile().mkdirs();
                playersFile.createNewFile();
            }

            List<String> currentPlayers;
            try (Stream<String> lines = Files.lines(playersFile.toPath())) {
                currentPlayers = lines.flatMap(line -> Stream.of(line.split(",\n")))
                        .map(String::trim)
                        .collect(Collectors.toList());
            }

            if (currentPlayers.contains(playerName)) {
                return "Player already exists.";
            }

            try (FileWriter writer = new FileWriter(playersFile, true)) {
                if (currentPlayers.isEmpty()) {
                    writer.write(playerName);
                } else {
                    writer.write(",\n" + playerName);
                }
            }

            return "Player added successfully.";
        } catch (IOException e) {
            throw new RuntimeException("Failed to write to players.txt file", e);
        }
    }

    //NEW
    //NEW
    //NEW


    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    @ExceptionHandler(JsonFileReadException.class)
    public String handleJsonFileReadException(JsonFileReadException e) {
        return e.getMessage();
    }

    static class JsonFileReadException extends RuntimeException {
        public JsonFileReadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    @SubscribeMapping("/loadJson")
    public String loadJson() throws IOException {
        Path path = Paths.get(JSON_FILE_PATH);
        byte[] jsonData = Files.readAllBytes(path);
        return new String(jsonData);
    }
}
