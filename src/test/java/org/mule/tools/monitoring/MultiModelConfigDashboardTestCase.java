package org.mule.tools.monitoring;

/**
 * @author David Dossot (david@dossot.net)
 */
public class MultiModelConfigDashboardTestCase extends
        AbstractDashboardTestCase {
    @Override
    protected String getConfigResources() {
        return "dashboardMultiModelConfig.xml";
    }

    public void testOnCallTwice() throws Exception {
        doTestOnCall("30", "gray");
        doTestOnCall("30", "lime");
    }
}
