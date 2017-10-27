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
        Admin admin = AdminAuthTest.createAdmin(); // creates admin user
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
        Admin admin = AdminAuthTest.createAdmin();
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
}
