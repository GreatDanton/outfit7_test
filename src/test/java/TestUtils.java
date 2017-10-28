import com.googlecode.objectify.ObjectifyService;

import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;

// custom imports
import com.clicktracker.model.Campaign;
import com.clicktracker.model.Platform;
import com.clicktracker.model.Counter;
import com.clicktracker.model.Click;
import com.clicktracker.model.Admin;

// TestUtils holds helper functions for setting up testing environment
// used in Unit & Integration tests
public class TestUtils {
    // helper function for creating admin in test functions
    public static Admin createAdmin() {
        Admin admin = new Admin("admin", "1234", true);
        ObjectifyService.ofy().save().entity(admin).now();
        return admin;
    }

    // helper function to create campaigns with different
    // arguments
    public static List<Campaign> createDummyCampaigns() {
        List<Platform> platforms = createPlatforms();
        Platform android = platforms.get(0);
        Platform iphone = platforms.get(1);
        List<Long> both = new ArrayList<Long>();
        both.add(android.id);
        both.add(iphone.id);

        List<Long> androidOnly = new ArrayList<Long>();
        androidOnly.add(android.id);

        List<Long> iphoneOnly = new ArrayList<Long>();
        iphoneOnly.add(iphone.id);

        Campaign c1 = new Campaign("My first campaign", "http://www.myfirstcampaign.com", both, true, new Date());
        Campaign c2 = new Campaign("My second campaign", "http://www.mysecondcampaign.com", androidOnly, true,
                new Date());
        Campaign c3 = new Campaign("My third campaign", "http://www.mythirdcampaign.com", androidOnly, false,
                new Date());
        Campaign c4 = new Campaign("My fourth campaign", "http://www.myfourthcampaign.com", iphoneOnly, true,
                new Date());

        List<Campaign> allCampaigns = Arrays.asList(c1, c2, c3, c4);
        ObjectifyService.ofy().save().entities(c1, c2, c3, c4).now();
        return allCampaigns;
    }

    // helper function to create android, iphone, platforms
    public static List<Platform> createPlatforms() {
        Platform p1 = new Platform("android");
        Platform p2 = new Platform("iphone");
        ObjectifyService.ofy().save().entities(p1, p2).now();
        List<Platform> platforms = new ArrayList<Platform>();
        platforms.add(p1);
        platforms.add(p2);
        return platforms;
    }

}