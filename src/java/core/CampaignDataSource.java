package core;


import java.io.IOException;

public interface CampaignDataSource {
    CampaignData load() throws IOException;
}
