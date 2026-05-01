package ovh.roro.libraries.paperrunner;

import com.google.common.hash.Hashing;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import kong.unirest.core.HttpResponse;
import kong.unirest.core.Unirest;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public class PaperRunner {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("You must provide a version! Example: java -jar paper-runner.jar 1.21.11");
            return;
        }

        Gson gson = new Gson();
        String version = args[0];
        String buildsUrl = "https://fill.papermc.io/v3/projects/paper/versions/%s/builds".formatted(version);
        String userAgent = "Java/Paper-Runner 1.2.0 (https://github.com/roro1506HD/paper-runner)";

        HttpResponse<String> buildsResponse = Unirest.get(buildsUrl)
                .header("User-Agent", userAgent)
                .header("Accept", "application/json")
                .asString();

        String buildsResponseBody = buildsResponse.getBody();

        if (buildsResponse.getStatus() != 200) {
            System.out.println("An error occurred when fetching builds (%d):".formatted(buildsResponse.getStatus()));
            System.out.println(buildsResponseBody);
            return;
        }

        JsonArray builds = gson.fromJson(buildsResponseBody, JsonArray.class);
        JsonObject latestBuild = null;
        int latestBuildNumber = -1;

        for (JsonElement element : builds) {
            JsonObject build = element.getAsJsonObject();
            int buildNumber = build.get("id").getAsInt();

            if (buildNumber > latestBuildNumber) {
                latestBuild = build;
                latestBuildNumber = buildNumber;
            }
        }

        if (latestBuild == null) {
            System.out.println("Couldn't find any build for this version!");
            return;
        }

        JsonObject downloads = latestBuild.get("downloads").getAsJsonObject();
        JsonObject downloadEntry = downloads.get("server:default").getAsJsonObject();
        String fileName = downloadEntry.get("name").getAsString();
        String hash = downloadEntry.getAsJsonObject("checksums").get("sha256").getAsString();
        Path paperJar = Path.of("paper-server.jar");

        try {
            boolean downloadLatestBuild = false;

            if (Files.exists(paperJar)) {
                byte[] fileBytes = Files.readAllBytes(paperJar);
                String currentHash = Hashing.sha256().hashBytes(fileBytes).toString();

                if (currentHash.equals(hash)) {
                    System.out.println("Current paper-server.jar hash matches latest build hash, continuing!");
                } else {
                    System.out.println("Current paper-server.jar hash does not match latest build hash, downloading latest build!");
                    System.out.println("Current hash: " + currentHash);
                    System.out.println("Expected hash: " + hash);

                    Files.delete(paperJar);

                    downloadLatestBuild = true;
                }
            } else {
                System.out.println("paper-server.jar not found, downloading latest build!");

                downloadLatestBuild = true;
            }

            if (downloadLatestBuild) {
                HttpResponse<byte[]> downloadResponse = Unirest.get("https://fill-data.papermc.io/v1/objects/" + hash + "/" + fileName)
                        .header("User-Agent", userAgent)
                        .asBytes();

                if (downloadResponse.getStatus() != 200) {
                    System.out.println("An error occurred when downloading " + fileName + " (" + downloadResponse.getStatus() + ")");
                    return;
                }

                Files.write(paperJar, downloadResponse.getBody());

                System.out.println("Successfully downloaded paper " + version + " build " + latestBuildNumber + "! Continuing!");
            }

            Path startScript = Path.of("start.sh");
            if (!Files.exists(startScript)) {
                System.out.println("Start script not found, creating it");

                try (
                        BufferedReader reader = new BufferedReader(new InputStreamReader(Objects.requireNonNull(PaperRunner.class.getResourceAsStream("/start.sh"))));
                        PrintWriter writer = new PrintWriter(Files.newBufferedWriter(startScript))
                ) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        writer.println(line.replace("%%VERSION%%", version));
                    }
                }

                System.out.println("Start script created, to run both paper-runner.jar and paper-server.jar, please use the start script!");
            }
        } catch (Exception ex) {
            System.out.println("An error occurred:");
            //noinspection CallToPrintStackTrace
            ex.printStackTrace();
        }
    }
}
