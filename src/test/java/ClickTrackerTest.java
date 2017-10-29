import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cache.AsyncCacheFilter;
import com.googlecode.objectify.util.Closeable;

// import testing packages
import org.mockito.Mockito;
import org.mockito.ArgumentCaptor;

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

import com.clicktracker.ClickTrackerServlet;
// custom imports
import com.clicktracker.model.Campaign;
import com.clicktracker.model.Platform;
import com.clicktracker.model.Counter;
import com.clicktracker.model.Click;

// ClickTrackerTest is used to test ClickTrackerServlet in com. folder
public class ClickTrackerTest {
    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig());
    protected Closeable session;

    private HttpServletRequest mockRequest;
    private HttpServletResponse mockResponse;

    // call responseWriter.toString() to get servlet response
    private StringWriter responseWriter = new StringWriter();
    private PrintWriter printWriter = new PrintWriter(responseWriter);

    @BeforeClass
    public static void setupBeforeClass() {
        ObjectifyService.setFactory(new ObjectifyFactory());
        // register
        ObjectifyService.register(Platform.class);
        ObjectifyService.register(Click.class);
        ObjectifyService.register(Counter.class);
        ObjectifyService.register(Campaign.class);
    }

    @Before
    public void setUp() throws IOException {
        this.session = ObjectifyService.begin();
        this.helper.setUp();

        mockRequest = Mockito.mock(HttpServletRequest.class);
        mockResponse = Mockito.mock(HttpServletResponse.class);
        Mockito.when(mockResponse.getWriter()).thenReturn(printWriter);
    }

    @After
    public void tearDown() {
        AsyncCacheFilter.complete();
        this.session.close();
        this.helper.tearDown();
    }

    // sets up campaign table that could be used in integration tests below
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

    // testing client get request on campaign id that do exist in db
    // - client should be redirected to redirectURL in db
    @Test
    public void doGet_Test() throws IOException {
        createTestCampaign();
        Campaign c = ObjectifyService.ofy().load().type(Campaign.class).first().now();
        Mockito.when(mockRequest.getPathInfo()).thenReturn("/" + String.valueOf(c.id));

        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        new ClickTrackerServlet().doGet(mockRequest, mockResponse);

        Mockito.verify(mockResponse).sendRedirect(argumentCaptor.capture());
        assertEquals(c.redirectURL, argumentCaptor.getValue());
    }

    // testing client get request on the campaign id that do not exist
    // client should be redirected to default redirect url (outfit7.com);
    @Test
    public void doGetTest_404() throws IOException {
        createTestCampaign();
        Mockito.when(mockRequest.getPathInfo()).thenReturn("/nonExistentCampaignID");
        new ClickTrackerServlet().doGet(mockRequest, mockResponse);
        // capture send redirect url
        ArgumentCaptor<String> argumentCaptor = ArgumentCaptor.forClass(String.class);
        Mockito.verify(mockResponse).sendRedirect(argumentCaptor.capture());
        assertEquals("http://www.outfit7.com", argumentCaptor.getValue());
    }

    // create post request on url with campaignID that exist in the database
    // - check if client gets correct redirectURL in response
    // - check if counting clicks is working correctly
    @Test
    public void doPost_Test() throws IOException {
        // create platform table and campaign table
        createTestCampaign();
        Campaign c = ObjectifyService.ofy().load().type(Campaign.class).first().now();
        Mockito.when(mockRequest.getPathInfo()).thenReturn("/" + String.valueOf(c.id));

        // simulate client click
        new ClickTrackerServlet().doPost(mockRequest, mockResponse);

        String data = responseWriter.toString();
        // check if post mockRequest mockResponse contains redirectURL and campaign url
        assertTrue(data.contains("redirectURL"));
        assertTrue(data.contains(c.redirectURL));

        // check if number of rows in click table is the same as expected
        // 1 == 1
        Integer dbClickNum = ObjectifyService.ofy().load().type(Click.class).filter("campaignID", c.id).count();
        Long expectedNumOfClicks = 1L;
        assertEquals(expectedNumOfClicks, new Long(dbClickNum));

        // creating new post mockRequest, check if number of clicks match
        // 2 == 2
        new ClickTrackerServlet().doPost(mockRequest, mockResponse);
        expectedNumOfClicks++;
        // check numbers from db again
        Integer dbClickNum2 = ObjectifyService.ofy().load().type(Click.class).filter("campaignID", c.id).count();
        assertEquals(expectedNumOfClicks, new Long(dbClickNum2));

        // check if counter numbers match with expected number of clicks
        // ie: 2 == 2
        Counter counter = ObjectifyService.ofy().load().type(Counter.class).filter("campaignID", c.id).first().now();
        assertEquals(expectedNumOfClicks, counter.numOfClicks);
    }

    // check post request on campaign id that does not exist
    // client should get default redirectURL (www.outfit7.com) in json response
    @Test
    public void doPost_404() throws IOException {
        Mockito.when(mockRequest.getPathInfo()).thenReturn("/nonExistentCampaignID");
        new ClickTrackerServlet().doPost(mockRequest, mockResponse);
        Click click = ObjectifyService.ofy().load().type(Click.class).first().now();
        if (click != null) {
            assertTrue(false); // click should not be registered
        }

        String data = responseWriter.toString();
        assertTrue(data.contains("statusCode"));
        assertTrue(data.contains("404"));
        assertTrue(data.contains("http://www.outfit7.com"));
    }

    // check if storeClick function is counting clicks correctly
    @Test
    public void storeClick_Test() throws IOException {
        // save platform in platform table
        Platform p = new Platform("android");
        ObjectifyService.ofy().save().entity(p).now();
        List<Long> platforms = new ArrayList<Long>();
        platforms.add(p.id);

        // create campaign
        Campaign c = new Campaign("My campaign", "https://www.outfit7.com", platforms, true, new Date());
        ObjectifyService.ofy().save().entity(c).now();
        Mockito.when(mockRequest.getPathInfo()).thenReturn("/" + String.valueOf(c.id));

        // simulate first click on the campaign
        new ClickTrackerServlet().storeClick(mockRequest);
        Long clicks = 1L;

        // count number of clicks in click table and compare it to clicks variable
        List<Click> clicksArr = ObjectifyService.ofy().load().type(Click.class).list();
        Long numOfClicks = new Long(clicksArr.size());
        assertEquals(clicks, numOfClicks);

        // check if number of clicks in counter is the same as expected
        Counter counter = ObjectifyService.ofy().load().type(Counter.class).filter("campaignID", c.id).first().now();
        assertEquals(clicks, counter.numOfClicks);

        // simulate another click, check if numbers match
        new ClickTrackerServlet().storeClick(mockRequest);
        clicks++;
        assertEquals(clicks, counter.numOfClicks);

        // count number of clicks in click table again and compare it with counter
        List<Click> clicksArr2 = ObjectifyService.ofy().load().type(Click.class).list();
        Long numOfClicks2 = new Long(clicksArr2.size());
        assertEquals(numOfClicks2, counter.numOfClicks);
    }

}