package com.arcadiadevs.viora.platform.shared.interfaces.rest.security;

import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.util.ClassUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Every endpoint reaching the data hanging from a plot, or a prescription by its id, checks the owner with method
 * security, so a new controller cannot forget it: that omission is what let other producers reach samplings,
 * certifications and prescriptions. The plot resource itself ({@code PlotController}) is left out: its command and
 * query services look the plot up by owner.
 */
class PlotRouteAuthorizationTest {

    private static final String BASE_PACKAGE = "com.arcadiadevs.viora.platform";

    @Test
    void everyRouteUnderAPlotChecksItsOwner() throws Exception {
        assertEquals(List.of(), routesMissing("/plots/{plotId}/", "@plotAccess.isAccessible(#plotId)"));
    }

    @Test
    void everyRouteUnderAPrescriptionChecksItsOwner() throws Exception {
        assertEquals(List.of(), routesMissing("/thinning-prescriptions/{id}", "@prescriptionAccess.isAccessible(#id)"));
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
