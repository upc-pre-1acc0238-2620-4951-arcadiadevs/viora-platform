package com.arcadiadevs.viora.platform.shared.interfaces.rest.security;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/**
 * Every endpoint reaching the data hanging from a plot, or a prescription by its id, checks the owner with method
 * security, so a new controller cannot forget it: that omission is what let other producers reach samplings,
 * certifications and prescriptions. The plot resource itself ({@code PlotController}) is left out: its command and
 * query services look the plot up by owner.
 */
class PlotRouteAuthorizationTest {

    private static final String BASE_PACKAGE = "com.arcadiadevs.viora.platform";
    private static final String PLOT_ACCESS = "@plotAccess.isAccessible(#plotId)";

    @Test
    void everyRouteUnderAPlotChecksItsOwner() throws Exception {
        assertEquals(List.of(), routesMissing("/plots/{plotId}/", PLOT_ACCESS));
    }

    @Test
    void everyRouteUnderAPrescriptionChecksItsOwner() throws Exception {
        assertEquals(List.of(), routesMissing("/thinning-prescriptions/{id}", "@prescriptionAccess.isAccessible(#id)"));
    }

    /**
     * A route can also take the plot as a query parameter, as the assistant does. The rule is scoped to the
     * assistant: older controllers filtering by an optional {@code plotId} check the owner in their services.
     */
    @Test
    void everyAssistantRouteTakingAPlotParameterChecksItsOwner() throws Exception {
        var routes = new ArrayList<String>();
        var missing = new ArrayList<String>();
        for (var controller : controllers(BASE_PACKAGE + ".assistant")) {
            var classRule = AnnotatedElementUtils.findMergedAnnotation(controller, PreAuthorize.class);
            for (Method method : controller.getDeclaredMethods()) {
                if (AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class) == null
                        || !takesPlotIdParameter(method)) {
                    continue;
                }
                var route = controller.getSimpleName() + "." + method.getName();
                routes.add(route);
                var methodRule = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);
                var rule = methodRule != null ? methodRule : classRule;
                if (rule == null || !rule.value().contains(PLOT_ACCESS)) {
                    missing.add(route);
                }
            }
        }
        assertFalse(routes.isEmpty(), "the assistant exposes no route taking a plotId parameter");
        assertEquals(List.of(), missing);
    }

    private static boolean takesPlotIdParameter(Method method) {
        return Arrays.stream(method.getParameters()).anyMatch(parameter -> {
            var param = AnnotatedElementUtils.findMergedAnnotation(parameter, RequestParam.class);
            if (param == null) {
                return false;
            }
            var name = !param.name().isEmpty() ? param.name() : parameter.getName();
            return name.equals("plotId");
        });
    }

    private List<Class<?>> controllers(String basePackage) throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        var controllers = new ArrayList<Class<?>>();
        for (var candidate : scanner.findCandidateComponents(basePackage)) {
            controllers.add(ClassUtils.forName(candidate.getBeanClassName(), getClass().getClassLoader()));
        }
        return controllers;
    }

    private List<String> routesMissing(String pathFragment, String expression) throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        var missing = new ArrayList<String>();
        for (var candidate : scanner.findCandidateComponents(BASE_PACKAGE)) {
            var controller = ClassUtils.forName(candidate.getBeanClassName(), getClass().getClassLoader());
            if (controller.getSimpleName().equals("PlotController")) {
                continue;
            }
            var classPaths = paths(AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class));
            var classRule = AnnotatedElementUtils.findMergedAnnotation(controller, PreAuthorize.class);
            for (Method method : controller.getDeclaredMethods()) {
                var mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null || !reaches(classPaths, paths(mapping), pathFragment)) {
                    continue;
                }
                var methodRule = AnnotatedElementUtils.findMergedAnnotation(method, PreAuthorize.class);
                var rule = methodRule != null ? methodRule : classRule;
                if (rule == null || !rule.value().contains(expression)) {
                    missing.add(controller.getSimpleName() + "." + method.getName());
                }
            }
        }
        return missing;
    }

    private static List<String> paths(RequestMapping mapping) {
        if (mapping == null) {
            return List.of("");
        }
        var paths = mapping.path().length > 0 ? mapping.path() : mapping.value();
        return paths.length == 0 ? List.of("") : Arrays.asList(paths);
    }

    private static boolean reaches(List<String> classPaths, List<String> methodPaths, String pathFragment) {
        return classPaths.stream().anyMatch(base -> methodPaths.stream()
                .anyMatch(path -> (base + path).contains(pathFragment)));
    }
}
