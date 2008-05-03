package org.mule.tools.monitoring;

import org.mule.extras.client.MuleClient;
import org.mule.impl.MuleMessage;
import org.mule.tck.FunctionalTestCase;
import org.mule.umo.UMOException;

/**
 * @author David Dossot (david@dossot.net)
 */
public abstract class AbstractDashboardTestCase extends FunctionalTestCase {

    private static final String DASHBOARD_COMPONENT_NAME = "dashboard";

    private MuleClient muleClient;

    public AbstractDashboardTestCase() {
        super();
    }

    @Override
    protected void doPostFunctionalSetUp() throws Exception {
        super.doPostFunctionalSetUp();

        muleClient = new MuleClient();
    }

    protected void doTestOnCall(final String expectedRefreshPeriod,
            final String expectedColour) throws Exception, UMOException {

        final String dashboardContent = getDashboardContent();

        assertNotNull(dashboardContent);

        assertTrue("<html>", dashboardContent.contains("<html>"));
        assertTrue("</html>", dashboardContent.contains("</html>"));
        assertTrue("Refreshed period", dashboardContent.contains("content=\""
                + expectedRefreshPeriod + "\""));
        assertTrue("fooComponent", dashboardContent.contains("fooComponent"));
        assertFalse("no dashboard",
                dashboardContent.contains(DASHBOARD_COMPONENT_NAME));

        assertTrue(expectedColour, dashboardContent.contains(expectedColour));
    }

    private String getDashboardContent() throws Exception, UMOException {
        return muleClient.send("vm://dashboard", new MuleMessage(null)).getPayloadAsString();
    }

}