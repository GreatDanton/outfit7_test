import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFactory;
import com.googlecode.objectify.ObjectifyService;
import com.googlecode.objectify.cache.AsyncCacheFilter;
import com.googlecode.objectify.util.Closeable;

// import testing packages
import org.mockito.Mockito;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNotEquals;
import org.junit.Test;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.Arrays;

import com.clicktracker.AdminServlet;
// custom imports
import com.clicktracker.model.Campaign;
import com.clicktracker.model.Platform;
import com.clicktracker.model.Counter;
import com.clicktracker.model.Click;
import com.clicktracker.model.Admin;

// AdminServletTest class is used to test behaviour of AdminServlet methods
public class AdminServletTest {
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
        ObjectifyService.register(Admin.class);
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

    // tests checkCredentials helper function (if admin is logged in).
    @Test
    public void checkCredentialsTest() throws IOException {
        Admin admin = TestUtils.createAdmin(); // creates admin user
        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(mockRequest.getSession(false)).thenReturn(session);
        Mockito.when(session.getAttribute("adminID")).thenReturn(admin.id);

        Boolean loggedIn = new AdminServlet().checkCredentials(mockRequest);
        // if ok == true; admin id is valid
        // if ok == false; admin id does not exist
        assertTrue(loggedIn);
    }

    // check credentials when the client has forged
    @Test
    public void checkCredentials_wrongID_Test() throws IOException {
        Admin admin = TestUtils.createAdmin();
        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(mockRequest.getSession(false)).thenReturn(session);
        // using wrong admin id
        Mockito.when(mockRequest.getAttribute("adminID")).thenReturn("wrongID");

        Boolean loggedIn = new AdminServlet().checkCredentials(mockRequest);
        // loggedIn == false, !loggedIn ==> true
        assertTrue(!loggedIn);
    }

    // if no session is present admin is not logged in
    @Test
    public void checkCredentials_noSession_Test() throws IOException {
        Boolean loggedIn = new AdminServlet().checkCredentials(mockRequest);
        assertTrue(!loggedIn);
    }

    // testing if platforms in datastore are displayed when calling
    // displayAllPlatforms function
    @Test
    public void displayAllPlatforms_Test() throws IOException {
        Platform p1 = new Platform("android");
        Platform p2 = new Platform("iphone");
        ObjectifyService.ofy().save().entities(p1, p2).now();

        new AdminServlet().displayAllPlatforms(mockRequest, mockResponse);
        String output = responseWriter.toString();
        assertTrue(output.contains("platforms"));
        assertTrue(output.contains("android"));
        assertTrue(output.contains("iphone"));
    }

    // testing displayAllPlatforms function
    @Test
    public void displayAllPlatforms_Test2() throws IOException {
        Platform p = new Platform("android");
        ObjectifyService.ofy().save().entity(p).now();

        new AdminServlet().displayAllPlatforms(mockRequest, mockResponse);
        String output = responseWriter.toString();
        assertTrue(output.contains("android"));
        assertTrue(!output.contains("iphone"));
    }

    // testing filterCampaigns function
    // TODO: split this function into multiple smaller functions
    // => easier to track specific error
    @Test
    public void filterCampaigns_Test() throws IOException {
        List<Campaign> campaigns = TestUtils.createDummyCampaigns(); // create 4 campaigns
        Platform android = ObjectifyService.ofy().load().type(Platform.class).filter("name", "android").first().now();
        Platform iphone = ObjectifyService.ofy().load().type(Platform.class).filter("name", "iphone").first().now();

        List<Campaign> androidOnly = new AdminServlet().filterCampaigns(campaigns, "android");
        assertEquals(3, androidOnly.size());
        for (Campaign c : androidOnly) {
            assertTrue(c.platforms.contains(android.id));
        }

        List<Campaign> iphoneOnly = new AdminServlet().filterCampaigns(campaigns, "iphone");
        assertEquals(2, iphoneOnly.size());
        for (Campaign c : iphoneOnly) {
            assertTrue(c.platforms.contains(iphone.id));
        }

        List<Campaign> emptyCampaigns = new ArrayList<Campaign>();
        List<Campaign> empty = new AdminServlet().filterCampaigns(emptyCampaigns, "android");
        assertTrue(empty.isEmpty());

        List<Campaign> bothPlatforms = new AdminServlet().filterCampaigns(campaigns, "android, iphone");
        assertEquals(1, bothPlatforms.size());
        for (Campaign c : bothPlatforms) {
            assertTrue(c.platforms.contains(iphone.id));
            assertTrue(c.platforms.contains(android.id));
        }

        String bothPlatformsString = String.valueOf(android.id) + ", " + String.valueOf(iphone.id);
        List<Campaign> bothPlatforms2 = new AdminServlet().filterCampaigns(campaigns, bothPlatformsString);
        assertEquals(1, bothPlatforms2.size());
        for (Campaign c : bothPlatforms2) {
            assertTrue(c.platforms.contains(iphone.id));
            assertTrue(c.platforms.contains(android.id));
        }

        List<Campaign> nullCampaign = new AdminServlet().filterCampaigns(null, "");
        assertNull(nullCampaign);
    }

    // testing deleteCampaign function
    @Test
    public void deleteCampaign_Test() throws IOException {
        List<Campaign> campaigns = TestUtils.createDummyCampaigns();
        Campaign c1 = campaigns.get(0);
        Admin admin = TestUtils.createAdmin(); // creates admin user
        // make checkCredentials function happy
        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(mockRequest.getSession(false)).thenReturn(session);
        Mockito.when(session.getAttribute("adminID")).thenReturn(admin.id);
        Mockito.when(mockRequest.getPathInfo()).thenReturn("/" + String.valueOf(c1.id));

        Long cID = c1.id;
        // delete first campaign;
        new AdminServlet().doDelete(mockRequest, mockResponse);
        // check if campaign was deleted
        Campaign deletedCampaign = ObjectifyService.ofy().load().type(Campaign.class).id(cID).now();
        assertNull(deletedCampaign);
    }

    // testing delete campaign when admin is not logged in (session does not exist)
    @Test
    public void deleteCampaign_nosession_Test() throws IOException {
        List<Campaign> campaigns = TestUtils.createDummyCampaigns();
        Campaign c1 = campaigns.get(0);
        Admin admin = TestUtils.createAdmin();
        // no session
        Mockito.when(mockRequest.getPathInfo()).thenReturn("/" + String.valueOf(c1.id));
        // delete without session should fail
        new AdminServlet().doDelete(mockRequest, mockResponse);
        String output = responseWriter.toString();
        assertTrue(output.contains("Forbidden"));
    }

    // testing delete campaign when the campaign id does not exist in db
    @Test
    public void deleteCampaign_missingCampaignID_Test() throws IOException {
        List<Campaign> campaigns = TestUtils.createDummyCampaigns();
        Campaign c1 = campaigns.get(0);
        Admin admin = TestUtils.createAdmin();

        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(mockRequest.getSession(false)).thenReturn(session);
        Mockito.when(mockRequest.getPathInfo()).thenReturn("/missingCampaignID");
        Mockito.when(session.getAttribute("adminID")).thenReturn(admin.id);

        // delete without session should fail
        new AdminServlet().doDelete(mockRequest, mockResponse);
        String output = responseWriter.toString();
        assertTrue(output.contains("does not exist"));
    }

    // testing doPut function, if campaign fields are updated correctly
    @Test
    public void updateCampaign_Test() throws IOException {
        List<Campaign> campaigns = TestUtils.createDummyCampaigns();
        Campaign c1 = campaigns.get(0);
        String c1Name = c1.name;
        String c1URL = c1.redirectURL;
        Boolean c1Active = c1.active;
        Date c1createdAt = c1.createdAt;

        Admin admin = TestUtils.createAdmin();
        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(mockRequest.getSession(false)).thenReturn(session);
        Mockito.when(session.getAttribute("adminID")).thenReturn(admin.id);
        Mockito.when(mockRequest.getPathInfo()).thenReturn("/" + String.valueOf(c1.id));

        String newName = "My new name";
        String newURL = "http://newURL.eu";
        Mockito.when(mockRequest.getParameter("name")).thenReturn(newName);
        Mockito.when(mockRequest.getParameter("redirectURL")).thenReturn(newURL);

        new AdminServlet().doPut(mockRequest, mockResponse);
        // name and url parameters should be updated;
        Campaign updatedCampaign = ObjectifyService.ofy().load().type(Campaign.class).id(c1.id).now();
        assertEquals(c1Active, updatedCampaign.active);
        assertEquals(c1createdAt, updatedCampaign.createdAt);
        assertEquals(newName, updatedCampaign.name);
        assertEquals(newURL, updatedCampaign.redirectURL);

        // name & redirect url should be different
        assertNotEquals(c1Name, updatedCampaign.name);
        assertNotEquals(c1URL, updatedCampaign.redirectURL);
    }
}
