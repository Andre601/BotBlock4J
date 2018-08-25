import com.nathanwebb.BotBlock4J.BlockAuth;
import com.nathanwebb.BotBlock4J.BotBlockRequests;
import com.nathanwebb.BotBlock4J.BotList;
import com.nathanwebb.BotBlock4J.exceptions.EmptyResponseException;
import com.nathanwebb.BotBlock4J.exceptions.FailedToSendException;
import com.nathanwebb.BotBlock4J.exceptions.RatelimitedException;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

public class ApiTests {
    private BlockAuth blockAuth;
    @Test
    public void testPostRequest(){
        try {

            BufferedReader botListTokenReader = new BufferedReader(new FileReader("testing" + File.separator + "bfd_token.txt"));
            String bfdToken = botListTokenReader.readLine();
            botListTokenReader.close();

            blockAuth = new BlockAuth();
            blockAuth.setListAuthToken(BotList.BOTS_FOR_DISCORD, bfdToken);
            BotBlockRequests.postGuilds("460545687592108032",300, blockAuth);
        } catch (Exception | FailedToSendException | EmptyResponseException | RatelimitedException e){
            e.printStackTrace();
        }
    }
}