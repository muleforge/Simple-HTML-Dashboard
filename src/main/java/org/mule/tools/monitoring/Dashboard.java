package org.mule.tools.monitoring;

import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

import org.mule.MuleManager;
import org.mule.config.ConfigurationException;
import org.mule.config.i18n.CoreMessages;
import org.mule.config.i18n.MessageFactory;
import org.mule.impl.UMODescriptorAware;
import org.mule.management.stats.AllStatistics;
import org.mule.management.stats.ComponentStatistics;
import org.mule.umo.UMOComponent;
import org.mule.umo.UMODescriptor;
import org.mule.umo.UMOEventContext;
import org.mule.umo.lifecycle.Callable;
import org.mule.umo.lifecycle.Initialisable;
import org.mule.umo.lifecycle.InitialisationException;
import org.mule.umo.model.UMOModel;
import org.mule.util.StringUtils;

/**
 * A simple component that generates an HTML dashboard representing event flows
 * in a single Mule instance.
 * 
 * @author David Dossot (david@dossot.net)
 */
public final class Dashboard implements Initialisable, Callable,
        UMODescriptorAware {

    static final Integer DEFAULT_REFRESH_PERIOD = Integer.valueOf(60);

    private String dashboardComponentName;

    private String modelName;

    private String hostName;

    private Integer refreshPeriod;

    private String componentNameRegex;

    private Pattern componentNamePattern;

    private AllStatistics allStatistics;

    private UMOModel model;

    private final Set<String> observedComponents = new TreeSet<String>();

    private final ConcurrentMap<String, PreviousComponentStatistics> previousStatistics =
            new ConcurrentHashMap<String, PreviousComponentStatistics>();

    public void setDescriptor(final UMODescriptor descriptor)
            throws ConfigurationException {

        dashboardComponentName = descriptor.getName();

        modelName = (String) descriptor.getProperties().get("modelName");

        if (modelName == null) {
            modelName = descriptor.getModelName();
        }

        final String refreshPeriodAsString =
                (String) descriptor.getProperties().get("refreshPeriod");

        if (refreshPeriodAsString != null) {
            refreshPeriod = Integer.valueOf(refreshPeriodAsString);
        } else {
            refreshPeriod = DEFAULT_REFRESH_PERIOD;
        }

        componentNameRegex =
                (String) descriptor.getProperties().get("componentNameRegex");
    }

    public void initialise() throws InitialisationException {
        if (StringUtils.isBlank(modelName)) {
            throw new InitialisationException(
                    CoreMessages.objectIsNull("modelName"), this);
        }

        if (StringUtils.isNotBlank(componentNameRegex)) {
            componentNamePattern =
                    Pattern.compile(componentNameRegex,
                            Pattern.CASE_INSENSITIVE);
        }

        hostName = System.getProperty("host.name");

        final MuleManager muleManager = (MuleManager) MuleManager.getInstance();

        model = muleManager.lookupModel(modelName);

        if (model == null) {
            throw new InitialisationException(
                    MessageFactory.createStaticMessage("Can not locate a model named: "
                            + modelName), this);
        }

        allStatistics = muleManager.getStatistics();

        loadObservedComponents();
    }

    private void loadObservedComponents() {
        for (final Iterator<?> allComponentStatistics =
                allStatistics.getComponentStatistics().iterator(); allComponentStatistics.hasNext();) {

            final ComponentStatistics componentStatistics =
                    (ComponentStatistics) allComponentStatistics.next();

            final String componentName = componentStatistics.getName();

            if ((!componentName.equals(dashboardComponentName))
                    && ((componentNamePattern == null) || (componentNamePattern.matcher(componentName).matches()))) {
                observedComponents.add(componentName);
            }
        }
    }

    public Object onCall(final UMOEventContext eventContext) throws Exception {
        eventContext.getMessage().clearProperties();
        eventContext.getMessage().setStringProperty("Content-Type", "text/html");
        eventContext.setStopFurtherProcessing(true);
        return renderHtmlDashboard();
    }

    private String renderHtmlDashboard() {
        final StringBuilder sb = new StringBuilder("<html><head>").append("\n");
        sb.append(
                "<meta http-equiv=\"refresh\" content=\"" + refreshPeriod
                        + "\" />").append("\n");
        sb.append("</head><body><font size=\"2\">").append("\n");
        sb.append("<h3>").append(hostName).append(
                "</h3><table border=\"1\" cellpadding=\"1\" cellspacing=\"0\">").append(
                "\n");

        final Map<String, String> componentColors = getComponentColors();

        // output components in alpha order
        for (final String componentName : observedComponents) {
            final String componentColor = componentColors.get(componentName);

            sb.append("<tr><td><font size=\"2\">").append(componentName).append(
                    "&nbsp;</font></td><td bgcolor=\"").append(
                    componentColor != null ? componentColor : "black").append(
                    "\">").append(getComponentSymbol(componentName)).append(
                    "</td></tr>").append("\n");
        }

        sb.append("</table><br/>").append("\n");
        sb.append(new Date().toString()).append("\n");
        sb.append("</font></body></html>").append("\n");

        final String htmlDashboard = sb.toString();
        return htmlDashboard;
    }

    private String getComponentSymbol(final String componentName) {
        final UMOComponent component = model.getComponent(componentName);

        if (component == null) {
            return "&nbsp;?&nbsp;&nbsp;";
        } else if (!component.isStarted()) {
            return "&nbsp;X&nbsp;";
        } else if (component.isPaused()) {
            return "&nbsp;=&nbsp;";
        } else {
            return "&nbsp;&nbsp;&nbsp;&nbsp;";
        }
    }

    private Map<String, String> getComponentColors() {
        final Map<String, String> componentColors =
                new HashMap<String, String>();

        if (allStatistics == null) {
            return componentColors;
        }

        for (final Iterator<?> allComponentStatistics =
                allStatistics.getComponentStatistics().iterator(); allComponentStatistics.hasNext();) {

            final ComponentStatistics componentStatistics =
                    (ComponentStatistics) allComponentStatistics.next();

            final String componentName = componentStatistics.getName();

            if (observedComponents.contains(componentName)) {
                componentColors.put(componentName, getColorForStatistics(
                        previousStatistics.get(componentName),
                        componentStatistics));

                previousStatistics.put(componentName,
                        new PreviousComponentStatistics(componentStatistics));
            }
        }

        return componentColors;
    }

    private String getColorForStatistics(
            final PreviousComponentStatistics previousStatistics,
            final ComponentStatistics statistics) {

        if (previousStatistics != null) {

            if ((statistics.getExecutionErrors() > previousStatistics.getExecutionErrors())
                    || (statistics.getFatalErrors() > previousStatistics.getFatalErrors())) {
                return "red";
            }

            if (statistics.getAverageQueueSize() > previousStatistics.getAverageQueueSize()) {
                return "orange";
            }

            if (statistics.getExecutedEvents() > previousStatistics.getExecutedEvents()) {
                return "yellow";
            }

            return "lime";
        } else {
            return "gray";
        }
    }

    private static final class PreviousComponentStatistics {
        private final long averageQueueSize;

        private final long executedEvent;

        private final long executionError;

        private final long fatalError;

        public PreviousComponentStatistics(final ComponentStatistics statistics) {
            averageQueueSize = statistics.getAverageQueueSize();
            executedEvent = statistics.getExecutedEvents();
            executionError = statistics.getExecutionErrors();
            fatalError = statistics.getFatalErrors();
        }

        public long getAverageQueueSize() {
            return averageQueueSize;
        }

        public long getExecutedEvents() {
            return executedEvent;
        }

        public long getExecutionErrors() {
            return executionError;
        }

        public long getFatalErrors() {
            return fatalError;
        }
    }

}
