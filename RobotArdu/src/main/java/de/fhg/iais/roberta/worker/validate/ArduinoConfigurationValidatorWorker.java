package de.fhg.iais.roberta.worker.validate;

import com.google.common.collect.ClassToInstanceMap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import de.fhg.iais.roberta.bean.IProjectBean;
import de.fhg.iais.roberta.components.ConfigurationComponent;
import de.fhg.iais.roberta.components.Project;
import de.fhg.iais.roberta.typecheck.NepoInfo;
import de.fhg.iais.roberta.util.Key;
import de.fhg.iais.roberta.visitor.validate.AbstractProgramValidatorVisitor;
import de.fhg.iais.roberta.visitor.validate.ArduinoBrickValidatorVisitor;
import de.fhg.iais.roberta.worker.AbstractValidatorWorker;

public class ArduinoConfigurationValidatorWorker extends AbstractValidatorWorker {

    private static final Set<String> OVERLAPPING_PINS =
        Stream
            .of(
                "INPUT",
                "OUTPUT",
                "+",
                "S",
                "PULSE",
                "TRIG",
                "ECHO",
                "RED",
                "GREEN",
                "BLUE",
                "RST",
                "IN",
                "SDA",
                "RS",
                "E",
                "D4",
                "D5",
                "D6",
                "D7",
                "PIN1",
                "IN1",
                "IN2",
                "IN3",
                "IN4",
                "OUT")
            .collect(Collectors.toCollection(HashSet::new));

    private final List<String> freePins;

    public ArduinoConfigurationValidatorWorker(List<String> freePins) {
        this.freePins = Collections.unmodifiableList(freePins);
    }

    @Override
    public void execute(Project project) {
        List<String> currentFreePins = new ArrayList<>(this.freePins);
        project.getConfigurationAst().getConfigurationComponents().forEach((k, v) -> checkConfigurationBlock(v, project, currentFreePins));
        super.execute(project);
    }

    @Override
    protected AbstractProgramValidatorVisitor getVisitor(Project project, ClassToInstanceMap<IProjectBean.IBuilder<?>> beanBuilders) {
        return new ArduinoBrickValidatorVisitor(project.getConfigurationAst(), beanBuilders);
    }

    private static void checkConfigurationBlock(ConfigurationComponent configurationComponent, Project project, List<String> currentFreePins) {
        Map<String, String> componentProperties = configurationComponent.getComponentProperties();
        List<String> blockPins = new ArrayList<>();
        componentProperties.forEach((k, v) -> {
            if ( OVERLAPPING_PINS.contains(k) ) {
                if ( currentFreePins.contains(v) ) {
                    blockPins.add(v);
                    currentFreePins.removeIf(s -> s.equals(v));
                } else {
                    project.addToErrorCounter(1);
                    project.setResult(Key.PROGRAM_INVALID_STATEMETNS);
                    configurationComponent.addInfo(NepoInfo.error("CONFIGURATION_ERROR_OVERLAPPING_PORTS"));
                }
            }
        });
        if ( blockPins.stream().distinct().count() != blockPins.size() ) {
            project.addToErrorCounter(1);
            project.setResult(Key.PROGRAM_INVALID_STATEMETNS);
            configurationComponent.addInfo(NepoInfo.error("CONFIGURATION_ERROR_OVERLAPPING_PORTS"));
        }
    }
}
