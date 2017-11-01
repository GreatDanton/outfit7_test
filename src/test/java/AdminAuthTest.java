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

import com.clicktracker.AdminAuthServlet;
import com.clicktracker.AdminAuthServlet.Credentials;
// custom imports
import com.clicktracker.model.Campaign;
import com.clicktracker.model.Platform;
import com.clicktracker.model.Counter;
import com.clicktracker.model.Click;
import com.clicktracker.model.Admin;

// AdminAuthTest is used to test AdminAuthServlet class that handles admin
// login/logout functionality
public class AdminAuthTest {
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

    // test credentialsCheck function on correct name & password
    @Test
    public void credentialsCheck_Test() throws IOException {
        Admin admin = SetupUtils.createAdmin();
        // admin, 1234 are original admin name, password
        Mockito.when(mockRequest.getParameter("name")).thenReturn("admin");
        Mockito.when(mockRequest.getParameter("password")).thenReturn("1234");

        Credentials adminCred = new AdminAuthServlet().credentialsCheck(mockRequest, mockResponse);
        assertTrue(adminCred.ok);
        assertEquals(admin.id, adminCred.adminId);
    }

    // test checkCredentials function when wrong admin password is used
    @Test
    public void credentialsCheck_wrongPassword_Test() throws IOException {
        Admin admin = SetupUtils.createAdmin();
        Mockito.when(mockRequest.getParameter("name")).thenReturn("admin");
        // wrong password
        Mockito.when(mockRequest.getParameter("password")).thenReturn("321");

        Credentials adminCred = new AdminAuthServlet().credentialsCheck(mockRequest, mockResponse);
        assertTrue(!adminCred.ok);
    }

    // test checkCredentials function when wrong admin username is used
    @Test
    public void credentialsCheck_wrongUsername_Test() throws IOException {
        Admin admin = SetupUtils.createAdmin();
        // wrong name
        Mockito.when(mockRequest.getParameter("name")).thenReturn("_admin");
        Mockito.when(mockRequest.getParameter("password")).thenReturn("1234");

        Credentials adminCred = new AdminAuthServlet().credentialsCheck(mockRequest, mockResponse);
        assertTrue(!adminCred.ok);
    }

    // check admin login function when the password is correct
    @Test
    public void loginTest() throws IOException {
        Admin admin = SetupUtils.createAdmin();
        Mockito.when(mockRequest.getPathInfo()).thenReturn("/login");
        Mockito.when(mockRequest.getParameter("name")).thenReturn("admin");
        Mockito.when(mockRequest.getParameter("password")).thenReturn("1234");
        // mock user session
        HttpSession session = Mockito.mock(HttpSession.class);
        Mockito.when(mockRequest.getSession(true)).thenReturn(session);

        // try to login admin
        new AdminAuthServlet().doPost(mockRequest, mockResponse);
        String data = responseWriter.toString();
        assertTrue(data.contains("Successfully logged in"));
    }

    // check admin login when wrong password is used
    @Test
    public void loginTest_wrongPassword() throws IOException {
        Admin admin = SetupUtils.createAdmin();
        Mockito.when(mockRequest.getPathInfo()).thenReturn("/login");
        Mockito.when(mockRequest.getParameter("name")).thenReturn("admin");
        Mockito.when(mockRequest.getParameter("password")).thenReturn("321");

        // try to login admin
        new AdminAuthServlet().doPost(mockRequest, mockResponse);
        String data = responseWriter.toString();
        assertTrue(data.contains("Bad request"));
    }

    // check admin logout function
    @Test
    public void logoutTest() throws IOException {
        // first we have to login to test if logout is working
        Mockito.when(mockRequest.getPathInfo()).thenReturn("/logout");
        // mock user session
        HttpSession session = Mockito.mock(HttpSession.class);
        // we simulate admin that is already logged in
        Mockito.when(mockRequest.getSession(false)).thenReturn(session);

        new AdminAuthServlet().doPost(mockRequest, mockResponse);

        String data = responseWriter.toString();
        assertTrue(data.contains("Successfully logged out"));
    }

    // check logout if session is not present
    // - it should return bad request json message
    @Test
    public void logoutTest_noSession() throws IOException {
        Mockito.when(mockRequest.getPathInfo()).thenReturn("/logout");
        // we simulate client that is not logged in
        Mockito.when(mockRequest.getSession(false)).thenReturn(null);
        new AdminAuthServlet().doPost(mockRequest, mockResponse);

        String data = responseWriter.toString();
        assertTrue(data.contains("Bad request"));
    }
}