package org.mule.tools.monitoring;

/**
 * @author David Dossot (david@dossot.net)
 */
public class FullConfigDashboardTestCase extends AbstractDashboardTestCase {
    @Override
    protected String getConfigResources() {
        return "dashboardFullConfig.xml";
    }

    public void testOnCallTwice() throws Exception {
        doTestOnCall("30", "gray");
        doTestOnCall("30", "lime");
    }
}
