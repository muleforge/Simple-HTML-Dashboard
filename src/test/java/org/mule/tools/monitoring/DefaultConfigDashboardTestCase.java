package org.mule.tools.monitoring;

/**
 * @author David Dossot (david@dossot.net)
 */
public class DefaultConfigDashboardTestCase extends AbstractDashboardTestCase {
    @Override
    protected String getConfigResources() {
        return "dashboardShortConfig.xml";
    }

    public void testOnCallTwice() throws Exception {
        doTestOnCall("60", "gray");
        doTestOnCall("60", "lime");
    }
}
