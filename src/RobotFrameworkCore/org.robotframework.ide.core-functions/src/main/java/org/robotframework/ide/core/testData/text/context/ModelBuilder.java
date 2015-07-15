package org.robotframework.ide.core.testData.text.context;

import java.util.LinkedList;
import java.util.List;

import org.robotframework.ide.core.testData.model.LineElement.ElementType;
import org.robotframework.ide.core.testData.model.RobotTestDataFile;
import org.robotframework.ide.core.testData.text.context.ContextBuilder.ContextOutput;
import org.robotframework.ide.core.testData.text.context.ModelBuilder.ModelOutput.BuildMessage.Level;

import com.google.common.annotations.VisibleForTesting;


public class ModelBuilder {

    public ModelOutput build(final ContextOutput contexts) {
        ModelOutput output = new ModelOutput();

        List<AggregatedOneLineRobotContexts> lineContexts = contexts
                .getContexts();

        ElementType etLast = null;
        for (AggregatedOneLineRobotContexts ctx : lineContexts) {
            etLast = mapLine(output, ctx, etLast);
        }

        return output;
    }


    @VisibleForTesting
    protected ElementType mapLine(final ModelOutput model,
            final AggregatedOneLineRobotContexts ctx, final ElementType etLast) {
        RobotLineSeparatorsContexts separators = ctx.getSeparators();

        List<IContextElement> childContexts = ctx.getChildContexts();

        return etLast;
    }


    @VisibleForTesting
    protected IContextElementType getSeparatorType(
            final RobotLineSeparatorsContexts separators) {

        return null;
    }

    public static class ModelOutput {

        private final RobotTestDataFile fileModel;
        private final List<BuildMessage> buildMessages = new LinkedList<>();


        public ModelOutput() {
            this.fileModel = new RobotTestDataFile();
        }


        public ModelOutput(final RobotTestDataFile fileModel) {
            this.fileModel = fileModel;
        }


        public RobotTestDataFile getFileModel() {
            return fileModel;
        }


        public List<BuildMessage> getBuildMessages() {
            return buildMessages;
        }


        public void addBuildMessage(BuildMessage message) {
            buildMessages.add(message);
        }


        public boolean wasError() {
            boolean result = false;
            for (BuildMessage msg : buildMessages) {
                if (msg == null || msg.getType() == Level.ERROR) {
                    result = true;
                    break;
                }
            }

            return result;
        }

        public static class BuildMessage {

            private String message;
            private String localization;
            private Level type = Level.INFO;


            private BuildMessage(String message, String localization, Level type) {
                this.message = message;
                this.localization = localization;
                this.type = type;
            }


            public String getMessage() {
                return message;
            }


            public void setMessage(String message) {
                this.message = message;
            }


            public String getLocalization() {
                return localization;
            }


            public void setLocalization(String localization) {
                this.localization = localization;
            }


            public Level getType() {
                return type;
            }


            public void setType(Level type) {
                this.type = type;
            }


            public static BuildMessage buildInfo(String message,
                    String localization) {
                return new BuildMessage(message, localization, Level.INFO);
            }


            public static BuildMessage buildWarn(String message,
                    String localization) {
                return new BuildMessage(message, localization, Level.WARN);
            }


            public static BuildMessage buildError(String message,
                    String localization) {
                return new BuildMessage(message, localization, Level.ERROR);
            }

            public static enum Level {
                INFO, WARN, ERROR;
            }
        }

    }
}
