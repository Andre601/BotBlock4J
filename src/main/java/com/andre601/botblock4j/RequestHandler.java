/*
 * Copyright 2018 Nathan Webb (nathanwgithub@gmail.com), Andre601
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated
 * documentation files (the "Software"), to deal in the Software without restriction, including without limitation
 * the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software,
 * and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE
 * OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package com.andre601.botblock4j;

import com.andre601.botblock4j.exceptions.RatelimitedException;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import okhttp3.*;
import org.apache.commons.lang3.ObjectUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class RequestHandler {
    private ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    private final OkHttpClient CLIENT = new OkHttpClient();
    private final MediaType JSON = MediaType.get("application/json");

    public RequestHandler(){}

    /**
     * Shortcut method to either use {@link #postGuilds(ShardManager, BotBlockAPI)} or {@link #postGuilds(JDA, BotBlockAPI)}.
     * <br>ShardManager is prioritized over JDA.
     *
     * @param  botBlockAPI
     *        An instance of {@link com.andre601.botblock4j.BotBlockAPI BotBlockAPI}.
     *        <br>{@link com.andre601.botblock4j.BotBlockAPI.Builder BotBlockAPI.Builder} can be used to create an instance.
     *
     * @throws IllegalStateException
     *         When the post Request can't be performed (unknown error).
     * @throws IllegalAccessException
     *         When trying to use this method while both {@link net.dv8tion.jda.core.JDA JDA} and
     *         {@link net.dv8tion.jda.bot.sharding.ShardManager ShardManager} are null.
     * @throws IOException
     *         When the post Request couldn't be performed.
     * @throws RatelimitedException
     *         When the Bot (ID and/or IP) got ratelimited by the BotBlock-API.
     */
    public void postGuilds(BotBlockAPI botBlockAPI) throws IllegalStateException, IllegalAccessException, IOException, RatelimitedException{
        if(!ObjectUtils.anyNotNull(botBlockAPI.getJDA(), botBlockAPI.getShardManager()))
            throw new IllegalAccessException("JDA or ShardManager may not be null!");

        if(botBlockAPI.getShardManager() != null)
            postGuilds(botBlockAPI.getShardManager(), botBlockAPI);
        else
        if(botBlockAPI.getJDA() != null)
            postGuilds(botBlockAPI.getJDA(), botBlockAPI);
        else
            throw new IllegalStateException("Unknown issue while using postGuilds(BotBlockAPI)");
    }

    /**
     * Posts the Guilds from the provided {@link net.dv8tion.jda.core.JDA JDA instance} to the BotBlock-API.
     * <br>If the provided JDA is sharded (Has ShardInfo) then the shards ID and the total shard count will be posted too.
     *
     * @param jda
     *        The {@link net.dv8tion.jda.core.JDA JDA instance} to use.
     * @param botBlockAPI
     *        An instance of {@link com.andre601.botblock4j.BotBlockAPI BotBlockAPI}.
     *        <br>{@link com.andre601.botblock4j.BotBlockAPI.Builder BotBlockAPI.Builder} can be used to create an instance.
     *
     * @throws IllegalAccessException
     *         When trying to use this method while both {@link net.dv8tion.jda.core.JDA JDA} and
     *         {@link net.dv8tion.jda.bot.sharding.ShardManager ShardManager} are null.
     * @throws IOException
     *         When the post Request couldn't be performed.
     * @throws RatelimitedException
     *         When the Bot (ID and/or IP) got ratelimited by the BotBlock-API.
     */
    public void postGuilds(JDA jda, BotBlockAPI botBlockAPI) throws IllegalAccessException, IOException, RatelimitedException{
        if(!ObjectUtils.allNotNull(jda))
            throw new IllegalAccessException("JDA may not be null!");

        JSONObject json = new JSONObject()
                .put("server_count", jda.getGuildCache().size())
                .put("bot_id", jda.getSelfUser().getId());

        if(jda.getShardInfo() != null)
            json.put("shard_id", jda.getShardInfo().getShardId())
                    .put("shard_count", jda.getShardInfo().getShardTotal());

        botBlockAPI.getAuthTokens().forEach(json::put);

        performRequest(json);
    }

    /**
     * Posts the Guilds from the provided {@link net.dv8tion.jda.bot.sharding.ShardManager ShardManager} to the BotBlock-API.
     *
     * @param shardManager
     *        The {@link net.dv8tion.jda.bot.sharding.ShardManager ShardManager} to use.
     * @param botBlockAPI
     *        An instance of {@link com.andre601.botblock4j.BotBlockAPI BotBlockAPI}.
     *        <br>{@link com.andre601.botblock4j.BotBlockAPI.Builder BotBlockAPI.Builder} can be used to create an instance.
     *
     * @throws IllegalAccessException
     *         When trying to use this method while both {@link net.dv8tion.jda.core.JDA JDA} and
     *         {@link net.dv8tion.jda.bot.sharding.ShardManager ShardManager} are null.
     * @throws IOException
     *         When the post Request couldn't be performed.
     * @throws RatelimitedException
     *         When the Bot (ID and/or IP) got ratelimited by the BotBlock-API.
     */
    public void postGuilds(ShardManager shardManager, BotBlockAPI botBlockAPI) throws IllegalAccessException, IOException, RatelimitedException{
        if(!ObjectUtils.allNotNull(shardManager))
            throw new IllegalAccessException("ShardManager may not be null!");

        JSONObject json = new JSONObject()
                .put("server_count", shardManager.getGuildCache().size())
                .put("bot_id", shardManager.getShardById(0).getSelfUser().getId())
                .put("shard_count", shardManager.getShardsTotal());

        List<Integer> shards = new ArrayList<>();
        for(JDA jda : shardManager.getShards())
            shards.add((int)jda.getGuildCache().size());

        json.put("shards", new JSONArray(Arrays.deepToString(shards.toArray())));

        botBlockAPI.getAuthTokens().forEach(json::put);

        performRequest(json);
    }

    /**
     * Posts the provided Guilds for the provided Bot ID to the BotBlock-API.
     * <br>Consider using {@link #postGuilds(JDA, BotBlockAPI)} or {@link #postGuilds(ShardManager, BotBlockAPI)} instead.
     *
     * @param botId
     *        The ID of the bot.
     * @param guilds
     *        The guilds to post.
     * @param botBlockAPI
     *        An instance of {@link com.andre601.botblock4j.BotBlockAPI BotBlockAPI}.
     *        <br>{@link com.andre601.botblock4j.BotBlockAPI.Builder BotBlockAPI.Builder} can be used to create an instance.
     *
     * @throws IOException
     *         When the post Request couldn't be performed.
     * @throws RatelimitedException
     *         When the Bot (ID and/or IP) got ratelimited by the BotBlock-API.
     *
     * @see #postGuilds(String, int, BotBlockAPI) for the full method.
     */
    public void postGuilds(Long botId, int guilds, BotBlockAPI botBlockAPI) throws IOException, RatelimitedException{
        postGuilds(Long.toString(botId), guilds, botBlockAPI);
    }

    /**
     * Posts the provided Guilds for the provided Bot ID to the BotBlock-API.
     * <br>Consider using {@link #postGuilds(JDA, BotBlockAPI)} or {@link #postGuilds(ShardManager, BotBlockAPI)} instead.
     *
     * @param botId
     *        The ID of the bot.
     * @param guilds
     *        The guilds to post.
     * @param botBlockAPI
     *        An instance of {@link com.andre601.botblock4j.BotBlockAPI BotBlockAPI}.
     *        <br>{@link com.andre601.botblock4j.BotBlockAPI.Builder BotBlockAPI.Builder} can be used to create an instance.
     *
     * @throws IOException
     *         When the post Request couldn't be performed.
     * @throws RatelimitedException
     *         When the Bot (ID and/or IP) got ratelimited by the BotBlock-API.
     */
    public void postGuilds(String botId, int guilds, BotBlockAPI botBlockAPI) throws IOException, RatelimitedException{
        JSONObject json = new JSONObject()
                .put("server_count", guilds)
                .put("bot_id", botId);

        botBlockAPI.getAuthTokens().forEach(json::put);

        performRequest(json);
    }

    /**
     * Starts a Scheduler for posting the guilds each X minutes.
     * <br>The delay can be set with {@link com.andre601.botblock4j.BotBlockAPI.Builder#setUpdateInterval(int) BotBlockAPI.Builder#setUpdateInterval(int)}.
     *
     * @param botBlockAPI
     *        An instance of {@link com.andre601.botblock4j.BotBlockAPI BotBlockAPI}.
     *        <br>{@link com.andre601.botblock4j.BotBlockAPI.Builder BotBlockAPI.Builder} can be used to create an instance.
     *
     * @throws IllegalStateException
     *         This is thrown when one of the following things is the case:
     *         <ul>
     *             <li>{@link com.andre601.botblock4j.BotBlockAPI.Builder#disableJDARequirement(boolean) disableJDARequirement} was set to true.</li>
     *             <li>Both {@link net.dv8tion.jda.core.JDA JDA} and {@link net.dv8tion.jda.bot.sharding.ShardManager ShardManager} are null.</li>
     *             <li>When the Wrapper can't perfom the postRequest (Unknown Error)</li>
     *         </ul>
     */
    public void startAutoPost(BotBlockAPI botBlockAPI) throws IllegalStateException{

        if(botBlockAPI.isJdaDisabled())
            throw new IllegalStateException("startAutoPost can't be called when disableJDARequirement is true!");

        if(!ObjectUtils.anyNotNull(botBlockAPI.getJDA(), botBlockAPI.getShardManager()))
            throw new IllegalStateException("startAutoPost can't be called while JDA AND ShardManager are null!");

        scheduler.scheduleAtFixedRate(() -> {
            if(botBlockAPI.getShardManager() != null){
                try{
                    postGuilds(botBlockAPI.getShardManager(), botBlockAPI);
                }catch(IllegalAccessException | IOException | RatelimitedException ex){
                    ex.printStackTrace();
                }
            }else
            if(botBlockAPI.getJDA() != null){
                try{
                    postGuilds(botBlockAPI.getJDA(), botBlockAPI);
                }catch(IllegalAccessException | IOException | RatelimitedException ex){
                    ex.printStackTrace();
                }
            }else{
                throw new IllegalStateException("Unknown error while posting guilds.");
            }
        }, botBlockAPI.getUpdateInterval(), botBlockAPI.getUpdateInterval(), TimeUnit.MINUTES);
    }

    /**
     * Shuts down the Scheduler.
     */
    public void stopAutoPost(){
        scheduler.shutdown();
    }

    private void performRequest(JSONObject json) throws IOException, RatelimitedException{
        RequestBody requestBody = RequestBody.create(JSON, json.toString());

        Request request = new Request.Builder()
                .url("https://botblock.org/api/count")
                .addHeader("User-Agent", json.getString("bot_id"))
                .addHeader("Content-Type", "application/json") // Some sites require this in the header.
                .post(requestBody)
                .build();

        try(Response response = CLIENT.newCall(request).execute()){
            if(!response.isSuccessful()){
                if(response.body() == null)
                    throw new IOException("Received empty response-body from BotBlock.");

                if(response.code() == 429)
                    throw new RatelimitedException(response.body().string());

                throw new IOException("Couldn't post Guilds to BotBlock-API!");
            }

            if(response.body() == null)
                throw new IOException("Received empty response-body from BotBlock.");

            JSONObject responseJson = new JSONObject(response.body());
            if(!responseJson.get("failure").toString().equals("[]")){
                JSONObject failure = responseJson.getJSONObject("failure");
                List<String> sites = new ArrayList<>();

                for(String key : failure.keySet()){
                    try{
                        JSONArray failedList = failure.getJSONArray(key);
                        sites.add(String.format(
                                "Site name: %s, Error Code: %d, Error Message: %s",
                                key,
                                failedList.getInt(0),
                                failedList.getString(1)
                        ));
                    }catch(JSONException ex){
                        Map<String, Object> notFound = failure.toMap();
                        sites.add(String.format(
                                "Errors: %s",
                                notFound.toString()
                        ));
                    }
                }
                throw new IOException(String.format(
                        "One or multiple requests to post guild counts failed! Response: %s",
                        String.join(", ", sites)
                ));
            }
        }
    }

}
