package org.junit.runners.model;

import static java.lang.reflect.Modifier.isStatic;
import static org.junit.internal.MethodSorter.NAME_ASCENDING;

import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.internal.MethodSorter;

/**
 * Wraps a class to be run, providing method validation and annotation searching
 *
 * <p>
 *     这个类的作用就是解析被测试的类，只对有注解的方法和字段处理。注解类将作为Key，
 *     类的成员（字段和方法）会被包装为{@link FrameworkField}或{@link FrameworkMethod}实例的集合作为Value，记录到Map中。
 * </p>
 *
 * <p>
 *     主要记录的信息：
 *     <ul>
 *         <li>
 *             {@link #clazz}：被测试的类的Class实例。
 *         </li>
 *         <li>
 *             {@link #methodsForAnnotations}：Map类型，Key是方法上的注解，Value是方法集合，方法被{@link FrameworkMethod}包装。
 *             不可变的Map，不可变的集合。
 *         </li>
 *         <li>
 *             {@link #fieldsForAnnotations}：Map类型，Key是字段上的注解，Value是字段集合，字段被{@link FrameworkField}包装。
 *             不可变的Map，不可变的集合。
 *         </li>
 *
 *     </ul>
 *
 * </p>
 *
 * @since 4.5
 */
public class TestClass implements Annotatable {
    private static final FieldComparator FIELD_COMPARATOR = new FieldComparator();
    private static final MethodComparator METHOD_COMPARATOR = new MethodComparator();

    /**
     * 被测试的类的类实例
     */
    private final Class<?> clazz;
    /**
     * 测试时需要执行的方法的集合，Key为方法上的JUnit的注解，Value为方法的封装。
     * 包括 @Before @After @Test 等注解。
     */
    private final Map<Class<? extends Annotation>, List<FrameworkMethod>> methodsForAnnotations;
    private final Map<Class<? extends Annotation>, List<FrameworkField>> fieldsForAnnotations;

    /**
     * Creates a {@code TestClass} wrapping {@code clazz}. Each time this
     * constructor executes, the class is scanned for annotations, which can be
     * an expensive process (we hope in future JDK's it will not be.) Therefore,
     * try to share instances of {@code TestClass} where possible.
     */
    public TestClass(Class<?> clazz) {
        this.clazz = clazz;
        if (clazz != null && clazz.getConstructors().length > 1) {
            // 被测试的类只能有一个构造函数
            throw new IllegalArgumentException(
                    "Test class can only have one constructor");
        }

        Map<Class<? extends Annotation>, List<FrameworkMethod>> methodsForAnnotations =
                new LinkedHashMap<Class<? extends Annotation>, List<FrameworkMethod>>();
        Map<Class<? extends Annotation>, List<FrameworkField>> fieldsForAnnotations =
                new LinkedHashMap<Class<? extends Annotation>, List<FrameworkField>>();

        // 扫描被测试的类（包括父类）中有任何注解（不限于JUnit的注解）的方法和字段保存到methodsForAnnotations和fieldsForAnnotations中。
        scanAnnotatedMembers(methodsForAnnotations, fieldsForAnnotations);

        // 将methodsForAnnotations转成不可变的Map
        this.methodsForAnnotations = makeDeeplyUnmodifiable(methodsForAnnotations);
        // 将fieldsForAnnotations转成不可变的Map
        this.fieldsForAnnotations = makeDeeplyUnmodifiable(fieldsForAnnotations);
    }

    /**
     * 扫描被测试的类（包括父类）中有任何注解（不限于JUnit的注解）的方法和字段保存到methodsForAnnotations和fieldsForAnnotations中。
     * @param methodsForAnnotations
     * @param fieldsForAnnotations
     */
    protected void scanAnnotatedMembers(Map<Class<? extends Annotation>, List<FrameworkMethod>> methodsForAnnotations, Map<Class<? extends Annotation>, List<FrameworkField>> fieldsForAnnotations) {
        // 迭代被测试的类及父类父父类
        for (Class<?> eachClass : getSuperClasses(clazz)) {
            // 迭代类中的所有方法
            for (Method eachMethod : MethodSorter.getDeclaredMethods(eachClass)) {
                // 将每个有注解的方法保存到methodsForAnnotations中，任何注解，不限于JUnit的注解
                addToAnnotationLists(new FrameworkMethod(eachMethod), methodsForAnnotations);
            }
            // ensuring fields are sorted to make sure that entries are inserted
            // and read from fieldForAnnotations in a deterministic order
            for (Field eachField : getSortedDeclaredFields(eachClass)) {
                // 将每个有注解的字段保存到fieldsForAnnotations中，任何注解，不限于JUnit的注解
                addToAnnotationLists(new FrameworkField(eachField), fieldsForAnnotations);
            }
        }
    }

    private static Field[] getSortedDeclaredFields(Class<?> clazz) {
        Field[] declaredFields = clazz.getDeclaredFields();
        Arrays.sort(declaredFields, FIELD_COMPARATOR);
        return declaredFields;
    }

    /**
     * 将类的成员（被JUnit统一抽象成为了FrameworkMember对象）上的注解为Key，类成员作为Value保存到Map中。
     * @param member
     * @param map
     * @param <T>
     */
    protected static <T extends FrameworkMember<T>> void addToAnnotationLists(T member,
            Map<Class<? extends Annotation>, List<T>> map) {
        // 迭代方法上的注解
        for (Annotation each : member.getAnnotations()) {
            // 注解的类型
            Class<? extends Annotation> type = each.annotationType();
            // 从map中得到注解对应的方法集合，如果没有则创建一个空集合
            List<T> members = getAnnotatedMembers(map, type, true);

            // 如果集合中已添加了此方法，则return
            if (member.isShadowedBy(members)) {
                return;
            }
            if (runsTopToBottom(type)) {
                members.add(0, member);
            } else {
                members.add(member);
            }
        }
    }

    /**
     * 将一个Map转为不可变的Map，其中每个Value转为不可变的集合。
     * @param source
     * @param <T>
     * @return
     */
    private static <T extends FrameworkMember<T>> Map<Class<? extends Annotation>, List<T>>
            makeDeeplyUnmodifiable(Map<Class<? extends Annotation>, List<T>> source) {
        LinkedHashMap<Class<? extends Annotation>, List<T>> copy =
                new LinkedHashMap<Class<? extends Annotation>, List<T>>();
        for (Map.Entry<Class<? extends Annotation>, List<T>> entry : source.entrySet()) {
            copy.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(copy);
    }

    /**
     * Returns, efficiently, all the non-overridden methods in this class and
     * its superclasses that are annotated}.
     * 
     * @since 4.12
     */
    public List<FrameworkMethod> getAnnotatedMethods() {
        List<FrameworkMethod> methods = collectValues(methodsForAnnotations);
        Collections.sort(methods, METHOD_COMPARATOR);
        return methods;
    }

    /**
     * Returns, efficiently, all the non-overridden methods in this class and
     * its superclasses that are annotated with {@code annotationClass}.
     */
    public List<FrameworkMethod> getAnnotatedMethods(
            Class<? extends Annotation> annotationClass) {
        return Collections.unmodifiableList(getAnnotatedMembers(methodsForAnnotations, annotationClass, false));
    }

    /**
     * Returns, efficiently, all the non-overridden fields in this class and its
     * superclasses that are annotated.
     * 
     * @since 4.12
     */
    public List<FrameworkField> getAnnotatedFields() {
        return collectValues(fieldsForAnnotations);
    }

    /**
     * Returns, efficiently, all the non-overridden fields in this class and its
     * superclasses that are annotated with {@code annotationClass}.
     */
    public List<FrameworkField> getAnnotatedFields(
            Class<? extends Annotation> annotationClass) {
        return Collections.unmodifiableList(getAnnotatedMembers(fieldsForAnnotations, annotationClass, false));
    }

    private <T> List<T> collectValues(Map<?, List<T>> map) {
        Set<T> values = new LinkedHashSet<T>();
        for (List<T> additionalValues : map.values()) {
            values.addAll(additionalValues);
        }
        return new ArrayList<T>(values);
    }

    /**
     *
     * @param map
     * @param type 注解的类型
     * @param fillIfAbsent 如果注解对应的集合为空，是否创建新集合
     * @param <T>
     * @return
     */
    private static <T> List<T> getAnnotatedMembers(Map<Class<? extends Annotation>, List<T>> map,
            Class<? extends Annotation> type, boolean fillIfAbsent) {
        if (!map.containsKey(type) && fillIfAbsent) {
            map.put(type, new ArrayList<T>());
        }
        List<T> members = map.get(type);
        return members == null ? Collections.<T>emptyList() : members;
    }

    private static boolean runsTopToBottom(Class<? extends Annotation> annotation) {
        return annotation.equals(Before.class)
                || annotation.equals(BeforeClass.class);
    }

    /**
     * 将被测试的类，及其父类，父父类等，封闭到集合中。
     * @param testClass
     * @return
     */
    private static List<Class<?>> getSuperClasses(Class<?> testClass) {
        ArrayList<Class<?>> results = new ArrayList<Class<?>>();
        Class<?> current = testClass;
        while (current != null) {
            results.add(current);
            current = current.getSuperclass();
        }
        return results;
    }

    /**
     * Returns the underlying Java class.
     */
    public Class<?> getJavaClass() {
        return clazz;
    }

    /**
     * Returns the class's name.
     */
    public String getName() {
        if (clazz == null) {
            return "null";
        }
        return clazz.getName();
    }

    /**
     * Returns the only public constructor in the class, or throws an {@code
     * AssertionError} if there are more or less than one.
     */

    public Constructor<?> getOnlyConstructor() {
        Constructor<?>[] constructors = clazz.getConstructors();
        Assert.assertEquals(1, constructors.length);
        return constructors[0];
    }

    /**
     * Returns the annotations on this class
     */
    public Annotation[] getAnnotations() {
        if (clazz == null) {
            return new Annotation[0];
        }
        return clazz.getAnnotations();
    }

    public <T extends Annotation> T getAnnotation(Class<T> annotationType) {
        if (clazz == null) {
            return null;
        }
        return clazz.getAnnotation(annotationType);
    }

    public <T> List<T> getAnnotatedFieldValues(Object test,
            Class<? extends Annotation> annotationClass, Class<T> valueClass) {
        List<T> results = new ArrayList<T>();
        for (FrameworkField each : getAnnotatedFields(annotationClass)) {
            try {
                Object fieldValue = each.get(test);
                if (valueClass.isInstance(fieldValue)) {
                    results.add(valueClass.cast(fieldValue));
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(
                        "How did getFields return a field we couldn't access?", e);
            }
        }
        return results;
    }

    public <T> List<T> getAnnotatedMethodValues(Object test,
            Class<? extends Annotation> annotationClass, Class<T> valueClass) {
        List<T> results = new ArrayList<T>();
        for (FrameworkMethod each : getAnnotatedMethods(annotationClass)) {
            try {
                /*
                 * A method annotated with @Rule may return a @TestRule or a @MethodRule,
                 * we cannot call the method to check whether the return type matches our
                 * expectation i.e. subclass of valueClass. If we do that then the method 
                 * will be invoked twice and we do not want to do that. So we first check
                 * whether return type matches our expectation and only then call the method
                 * to fetch the MethodRule
                 */
                if (valueClass.isAssignableFrom(each.getReturnType())) {
                    Object fieldValue = each.invokeExplosively(test);
                    results.add(valueClass.cast(fieldValue));
                }
            } catch (Throwable e) {
                throw new RuntimeException(
                        "Exception in " + each.getName(), e);
            }
        }
        return results;
    }

    public boolean isPublic() {
        return Modifier.isPublic(clazz.getModifiers());
    }

    public boolean isANonStaticInnerClass() {
        return clazz.isMemberClass() && !isStatic(clazz.getModifiers());
    }

    @Override
    public int hashCode() {
        return (clazz == null) ? 0 : clazz.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        TestClass other = (TestClass) obj;
        return clazz == other.clazz;
    }

    /**
     * Compares two fields by its name.
     */
    private static class FieldComparator implements Comparator<Field> {
        public int compare(Field left, Field right) {
            return left.getName().compareTo(right.getName());
        }
    }

    /**
     * Compares two methods by its name.
     */
    private static class MethodComparator implements
            Comparator<FrameworkMethod> {
        public int compare(FrameworkMethod left, FrameworkMethod right) {
            return NAME_ASCENDING.compare(left.getMethod(), right.getMethod());
        }
    }
}
