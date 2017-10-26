import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.cache.AsyncCacheFilter;
import com.googlecode.objectify.util.Closeable;

// import testing packages
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;

// custom imports
import com.clicktracker.model.Platform;
import com.clicktracker.Utilities;

public class UtilitiesTest {
    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
    protected Closeable session;

    @BeforeClass
    public static void setupBeforeClass() {
        ObjectifyService.setFactory(new ObjectifyFactory());
        // register database
        ObjectifyService.register(Platform.class);
    }

    @Before
    public void setUP() {
        this.session = ObjectifyService.begin();
        this.helper.setUp();
    }

    @After
    public void tearDown() {
        AsyncCacheFilter.complete();
        this.session.close();
        this.helper.tearDown();
    }

    @Test
    public void getURLEndingTest() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        // pathInfo returns just the last part of the url
        // /api/v1/admin => returns /admin
        // that's why we are adding only the last part of url
        Mockito.when(request.getPathInfo()).thenReturn("/admin");
        String ending = Utilities.getURLEnding(request);
        assertEquals("admin", ending);
    }

    @Test
    public void getCampaignIDTest() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        // campaign ID:
        Mockito.when(request.getPathInfo()).thenReturn("/1234567890");
        Long id = Utilities.getCampaignID(request);
        Long expected = 1234567890L;
        assertEquals(expected, id);
    }

    @Test
    public void stringToLongTest() throws IOException {
        Long id = Utilities.stringToLong("123");
        Long expectedID = 123L;
        assertEquals(expectedID, id);
        // returned id should be null, since string could not
        // be parsed into long
        Long notID = Utilities.stringToLong("abcd");
        assertEquals(null, notID);
    }

    @Test
    public void getPlatformsTest() throws IOException {
        Platform p1 = new Platform("iphone");
        Platform p2 = new Platform("android");
        ObjectifyService.ofy().save().entity(p1).now();
        ObjectifyService.ofy().save().entity(p2).now();

        Long id1 = p1.id;
        Long id2 = p2.id;

        // return just one element => id of iphone platform
        // and compare it to the inserted element
        List<Long> plat1 = Utilities.getPlatforms("iphone");
        assertEquals(id1, plat1.get(0));

        List<Long> plat2 = Utilities.getPlatforms("android");
        assertEquals(id2, plat2.get(0));

        // windows does not exist in io exception => should return null
        List<Long> plat3 = Utilities.getPlatforms("windows");
        assertEquals(null, plat3);

        // check if iphone, android ids are returned in order
        List<Long> plat4 = Utilities.getPlatforms("iphone, android");
        List<Long> bothPlatforms = new ArrayList<Long>();
        bothPlatforms.add(id1);
        bothPlatforms.add(id2);
        for (int i = 0; i < plat4.size(); i++) {
            assertEquals(bothPlatforms.get(i), plat4.get(i));
        }

        List<Long> plat5 = Utilities.getPlatforms("android, iphone");
        List<Long> bothPlatforms2 = new ArrayList<Long>();
        bothPlatforms2.add(id2); // inserting first android
        bothPlatforms2.add(id1); // iphone
        // check if arrays are in order
        for (int i = 0; i < plat5.size(); i++) {
            assertEquals(bothPlatforms2.get(i), plat5.get(i));
        }

        List<Long> plat6 = Utilities.getPlatforms("iphone, android, windows");
        List<Long> platforms6 = new ArrayList<Long>();
        platforms6.add(id1);
        platforms6.add(id2);
        // only iphone and android should be in plat6.
        // windows is filtered out, because it does not exist in
        // objectify db
        for (int i = 0; i < plat6.size(); i++) {
            assertEquals(platforms6.get(i), plat6.get(i));
        }

        // parse ids from string and return them in list of longs
        // if the ids does not exist filter it out
        String stringOfIds = String.valueOf(id1) + ", " + String.valueOf(id2);
        List<Long> plat7 = Utilities.getPlatforms(stringOfIds);
        List<Long> platforms7 = new ArrayList<Long>();
        platforms7.add(id1);
        platforms7.add(id2);

        for (int i = 0; i < plat7.size(); i++) {
            assertEquals(platforms7.get(i), plat7.get(i));
        }
    }
}