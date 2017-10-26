
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
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import com.google.gson.Gson;

import com.clicktracker.ClickTrackerServlet;
// custom imports
import com.clicktracker.model.Campaign;
import com.clicktracker.model.Platform;
import com.clicktracker.model.Counter;
import com.clicktracker.model.Click;

public class ClicktrackerTest {
    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
    protected Closeable session;

    @BeforeClass
    public static void setupBeforeClass() {
        ObjectifyService.setFactory(new ObjectifyFactory());
        // register database
        ObjectifyService.register(Platform.class);
        ObjectifyService.register(Click.class);
        ObjectifyService.register(Counter.class);
        ObjectifyService.register(Campaign.class);

    }

    @Before
    public void setUp() {
        this.session = ObjectifyService.begin();
        this.helper.setUp();
    }

    @After
    public void tearDown() {
        AsyncCacheFilter.complete();
        this.session.close();
        this.helper.tearDown();
    }

    // check if storeClick function is counting clicks correctly
    @Test
    public void storeClickTest() throws IOException {
        // save platform in platform table
        Platform p = new Platform("android");
        ObjectifyService.ofy().save().entity(p).now();
        List<Long> platforms = new ArrayList<Long>();
        platforms.add(p.id);

        // create campaign
        Campaign c = new Campaign("My campaign", "https://www.outfit7.com", platforms, true, new Date());
        ObjectifyService.ofy().save().entity(c).now();

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);

        String campaignURL = "/" + String.valueOf(c.id);
        Mockito.when(request.getPathInfo()).thenReturn(campaignURL);

        // first click on the campaign
        ClickTrackerServlet.storeClick(request);

        // loading counter from database
        Counter counter = ObjectifyService.ofy().load().type(Counter.class).filter("campaignID", c.id).first().now();
        Long clicks = 1L;
        assertEquals(clicks, counter.numOfClicks);

        // check if number of clicks in click table is the same as the
        // number of clicks in counter
        List<Click> clicksArr = ObjectifyService.ofy().load().type(Click.class).list();
        Long numOfClicks = new Long(clicksArr.size());
        assertEquals(numOfClicks, counter.numOfClicks);

        // another click, check if numbers match
        ClickTrackerServlet.storeClick(request);
        clicks++;
        assertEquals(clicks, counter.numOfClicks);

        // count number of clicks in click table again and compare it to counter
        List<Click> clicksArr2 = ObjectifyService.ofy().load().type(Click.class).list();
        Long numOfClicks2 = new Long(clicksArr2.size());
        assertEquals(numOfClicks2, counter.numOfClicks);
    }

    @Test
    public void doPostTest() throws IOException {
        //https://stackoverflow.com/questions/5434419/how-to-test-my-servlet-using-junit

        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        // create platform table and campaign table
        createTestCampaign();
        Campaign c = ObjectifyService.ofy().load().type(Campaign.class).first().now();

        Mockito.when(request.getPathInfo()).thenReturn("/" + String.valueOf(c.id));
        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        Mockito.when(response.getWriter()).thenReturn(writer);

        // create post request on click tracker servlet
        new ClickTrackerServlet().doPost(request, response);

        writer.flush();
        String data = stringWriter.toString();

        // check if post request response contains redirectURL and campaign url
        assertTrue(data.contains("redirectURL"));
        assertTrue(data.contains(c.redirectURL));

        // check if number of rows in click table is the same as expected
        // 1 == 1
        Integer dbClickNum = ObjectifyService.ofy().load().type(Click.class).filter("campaignID", c.id).count();
        Long expectedNumOfClicks = 1L;
        assertEquals(expectedNumOfClicks, new Long(dbClickNum));

        // check if counter number is the same as expected number of clicks
        Counter counter = ObjectifyService.ofy().load().type(Counter.class).filter("campaignID", c.id).first().now();
        assertEquals(expectedNumOfClicks, counter.numOfClicks);

        // creating new post request, check if number of clicks match
        // 2 == 2
        new ClickTrackerServlet().doPost(request, response);
        expectedNumOfClicks++;
        // check numbers from db again
        Integer dbClickNum2 = ObjectifyService.ofy().load().type(Click.class).filter("campaignID", c.id).count();
        Counter counter2 = ObjectifyService.ofy().load().type(Counter.class).filter("campaignID", c.id).first().now();
        assertEquals(expectedNumOfClicks, new Long(dbClickNum2));
        assertEquals(expectedNumOfClicks, counter2.numOfClicks);

    }

    // check post request on campaign id that does not exist
    @Test
    public void checkPost404() throws IOException {
        HttpServletRequest request = Mockito.mock(HttpServletRequest.class);
        HttpServletResponse response = Mockito.mock(HttpServletResponse.class);

        Mockito.when(request.getPathInfo()).thenReturn("/akdjsakj");

        StringWriter stringWriter = new StringWriter();
        PrintWriter writer = new PrintWriter(stringWriter);
        Mockito.when(response.getWriter()).thenReturn(writer);

        new ClickTrackerServlet().doPost(request, response);
        writer.flush();
        String data = stringWriter.toString();

        assertTrue(data.contains("statusCode"));
        assertTrue(data.contains("404"));
        assertTrue(data.contains("http://www.outfit7.com"));
    }

    // sets up campaing table that could be used in integration tests
    public void createTestCampaign() {
        Platform p1 = new Platform("android");
        Platform p2 = new Platform("iphone");
        ObjectifyService.ofy().save().entity(p1).now();
        ObjectifyService.ofy().save().entity(p2).now();
        List<Long> platforms = new ArrayList<Long>();
        platforms.add(p1.id);
        platforms.add(p2.id);

        Campaign c = new Campaign("My test campaign", "http://www.google.com", platforms, true, new Date());
        ObjectifyService.ofy().save().entity(c).now();
    }

}