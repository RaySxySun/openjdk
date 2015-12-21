/*
 * Copyright (c) 2015, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * @test
 * @summary Test order of plugins
 * @author Jean-Francois Denise
 * @library ../../lib
 * @modules java.base/jdk.internal.jimage
 *          jdk.jdeps/com.sun.tools.classfile
 *          jdk.jlink/jdk.tools.jlink
 *          jdk.jlink/jdk.tools.jlink.internal
 *          jdk.jlink/jdk.tools.jmod
 *          jdk.jlink/jdk.tools.jimage
 *          jdk.compiler
 * @build tests.*
 * @run main/othervm PluginOrderTest
 */
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import jdk.tools.jlink.internal.ImagePluginConfiguration;
import jdk.tools.jlink.internal.PluginRepository;
import jdk.tools.jlink.internal.ImagePluginStack;
import jdk.tools.jlink.internal.PoolImpl;
import jdk.tools.jlink.api.Jlink.OrderedPluginConfiguration;
import jdk.tools.jlink.api.Jlink.PluginsConfiguration;
import jdk.tools.jlink.api.plugin.PluginException;
import jdk.tools.jlink.api.plugin.PluginProvider;
import jdk.tools.jlink.api.plugin.transformer.Pool;
import jdk.tools.jlink.api.plugin.transformer.TransformerCmdProvider;
import jdk.tools.jlink.api.plugin.transformer.TransformerPlugin;
import jdk.tools.jlink.api.plugin.transformer.TransformerPluginProvider;

import tests.Helper;
import tests.Result;

public class PluginOrderTest {

    public static void main(String[] args) throws Exception {
        new PluginOrderTest().test();
    }

    public void test() throws Exception {
        List<String> order = new ArrayList<>();
        PluginRepository.registerPluginProvider(new PProvider("plugin1_F",
                TransformerPluginProvider.FILTER, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin2_F",
                TransformerPluginProvider.FILTER, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin3_F",
                TransformerPluginProvider.FILTER, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin4_F",
                TransformerPluginProvider.FILTER, order));

        PluginRepository.registerPluginProvider(new PProvider("plugin1_T",
                TransformerPluginProvider.TRANSFORMER, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin2_T",
                TransformerPluginProvider.TRANSFORMER, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin3_T",
                TransformerPluginProvider.TRANSFORMER, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin4_T",
                TransformerPluginProvider.TRANSFORMER, order));

        PluginRepository.registerPluginProvider(new PProvider("plugin1_S",
                TransformerPluginProvider.SORTER, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin2_S",
                TransformerPluginProvider.SORTER, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin3_S",
                TransformerPluginProvider.SORTER, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin4_S",
                TransformerPluginProvider.SORTER, order));

        PluginRepository.registerPluginProvider(new PProvider("plugin1_C",
                TransformerPluginProvider.COMPRESSOR, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin2_C",
                TransformerPluginProvider.COMPRESSOR, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin3_C",
                TransformerPluginProvider.COMPRESSOR, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin4_C",
                TransformerPluginProvider.COMPRESSOR, order));

        PluginRepository.registerPluginProvider(new PProvider("plugin1_A",
                null, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin2_A",
                null, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin3_A",
                null, order));
        PluginRepository.registerPluginProvider(new PProvider("plugin4_A",
                null, order));

        test1(order);

        test2(order);

        test3(order);

        test4(order);

        test5(order);

        test6(order);

        test7(order);

        testAbs(order);

        test8(order);

        test9(order);
    }

    private void check(PluginsConfiguration config, List<String> expected, List<String> order)
            throws Exception {
        order.clear();
        ImagePluginStack plugins = ImagePluginConfiguration.parseConfiguration(config);
        PoolImpl pool = new PoolImpl();
        pool.add(Pool.newResource("/mod/com/foo/bar/A.somthing", new byte[0]));
        plugins.visitResources(pool);
        if (!order.equals(expected)) {
            throw new Exception("plugins not called in right order. Expected "
                    + expected + " actual " + order);
        }
        System.err.println("Gathered plugins: " + order);
    }

    private PluginsConfiguration createConfig(String... nameIndexAbs) {
        List<OrderedPluginConfiguration> lst = new ArrayList<>();
        for (String s : nameIndexAbs) {
            String name = s.substring(0, s.indexOf(":"));
            int sep = s.indexOf("/");
            int index = Integer.valueOf(s.substring(s.indexOf(":") + 1, sep));
            boolean absolute = Boolean.valueOf(s.substring(sep + 1));
            lst.add(new OrderedPluginConfiguration(name, index,
                    absolute, Collections.emptyMap()));
        }
        return new PluginsConfiguration(lst, Collections.emptyList(), null);
    }

    private void test1(List<String> order) throws Exception {

        Helper helper = Helper.newHelper();
        if (helper == null) {
            System.err.println("test1 not run");
            return;
        }
        helper.generateDefaultModules();

        {
            order.clear();
            String[] userOptions = {"--plugin1_C", "--plugin2_C", "--plugin1_S", "--plugin2_S", "--plugin1_T", "--plugin2_T", "--plugin1_F", "--plugin2_F"};
            String moduleName = "order1";
            helper.generateDefaultJModule(moduleName, "composite2");
            Result res = helper.generateDefaultImage(userOptions, moduleName);
            res.assertSuccess();
            if (!order.equals(Arrays.asList("plugin1_F", "plugin2_F", "plugin1_T", "plugin2_T", "plugin1_S", "plugin2_S", "plugin1_C", "plugin2_C"))) {
                throw new Exception("plugins not called in right order. " + order);
            }
        }

        {
            order.clear();
            String[] userOptions = {"--plugin1_F:2", "--plugin2_F:1"};
            String moduleName = "order2";
            helper.generateDefaultJModule(moduleName, "composite2");
            Result res = helper.generateDefaultImage(userOptions, moduleName);
            res.assertSuccess();
            if (!order.equals(Arrays.asList("plugin2_F", "plugin1_F"))) {
                throw new Exception("plugins not called in right order. " + order);
            }
        }

        {
            order.clear();
            String[] userOptions = {"--plugin1_C:LAST", "--plugin2_C:FIRST"};
            String moduleName = "order3";
            helper.generateDefaultJModule(moduleName, "composite2");
            Result res = helper.generateDefaultImage(userOptions, moduleName);
            res.assertSuccess();
            if (!order.equals(Arrays.asList("plugin2_C", "plugin1_C"))) {
                throw new Exception("plugins not called in right order. " + order);
            }
        }
    }

    private void test2(List<String> order) throws Exception {
        check(createConfig("plugin1_F:0/false", "plugin1_T:0/false", "plugin1_S:0/false",
                "plugin1_C:0/false", "plugin1_A:900000/true"),
                Arrays.asList("plugin1_F", "plugin1_T", "plugin1_S", "plugin1_C", "plugin1_A"), order);
    }

    private void test3(List<String> order) throws Exception {
        check(createConfig("plugin1_F:250/false", "plugin1_T:100/false", "plugin1_S:50/false",
                "plugin1_C:10/false", "plugin1_A:0/true"),
                Arrays.asList("plugin1_A", "plugin1_F", "plugin1_T", "plugin1_S", "plugin1_C"), order);
    }

    private void test4(List<String> order) throws Exception {
        check(createConfig("plugin2_F:0/false", "plugin3_F:1/false", "plugin4_F:2/false",
                "plugin1_F:3/false"),
                Arrays.asList("plugin2_F", "plugin3_F", "plugin4_F", "plugin1_F"), order);
    }

    private void test5(List<String> order) throws Exception {
        check(createConfig("plugin2_T:0/false", "plugin3_T:1/false", "plugin4_T:2/false",
                "plugin1_T:3/false"),
                Arrays.asList("plugin2_T", "plugin3_T", "plugin4_T", "plugin1_T"), order);
    }

    private void test6(List<String> order) throws Exception {
        check(createConfig("plugin2_S:0/false", "plugin3_S:1/false", "plugin4_S:2/false",
                "plugin1_S:3/false"),
                Arrays.asList("plugin2_S", "plugin3_S", "plugin4_S", "plugin1_S"), order);
    }

    private void test7(List<String> order) throws Exception {
        check(createConfig("plugin2_C:0/false", "plugin3_C:1/false", "plugin4_C:2/false",
                "plugin1_C:3/false"),
                Arrays.asList("plugin2_C", "plugin3_C", "plugin4_C", "plugin1_C"), order);
    }

    private void testAbs(List<String> order) throws Exception {
        check(createConfig("plugin2_A:0/true", "plugin3_A:1/true", "plugin4_A:2/true",
                "plugin1_A:3/true"),
                Arrays.asList("plugin2_A", "plugin3_A", "plugin4_A", "plugin1_A"), order);
    }

    private void test8(List<String> order) throws Exception {
        check(createConfig("plugin1_F:" + ImagePluginConfiguration.getRange(new CategoryProvider(TransformerPluginProvider.FILTER))[0] + "/true",
                "plugin1_T:" + ImagePluginConfiguration.getRange(new CategoryProvider(TransformerPluginProvider.TRANSFORMER))[0] + "/true",
                "plugin1_S:" + ImagePluginConfiguration.getRange(new CategoryProvider(TransformerPluginProvider.SORTER))[0] + "/true",
                "plugin1_C:" + ImagePluginConfiguration.getRange(new CategoryProvider(TransformerPluginProvider.COMPRESSOR))[0] + "/true"),
                Arrays.asList("plugin1_F", "plugin1_T", "plugin1_S", "plugin1_C"), order);
    }

    private void test9(List<String> order) throws Exception {
        List<String> expected = new ArrayList<>();
        expected.add("plugin1_F");
        expected.add("plugin2_F");
        expected.add("plugin1_T");
        expected.add("plugin2_T");
        expected.add("plugin1_S");
        expected.add("plugin2_S");
        expected.add("plugin1_C");
        expected.add("plugin2_C");

        check(createConfig("plugin1_F:0/false", "plugin1_T:0/false", "plugin1_S:0/false",
                "plugin1_C:0/false", "plugin2_F:" + (ImagePluginConfiguration.getRange(new CategoryProvider(TransformerPluginProvider.FILTER))[0] + 1) + "/true",
                "plugin2_T:" + (ImagePluginConfiguration.getRange(new CategoryProvider(TransformerPluginProvider.TRANSFORMER))[0] + 1) + "/true",
                "plugin2_S:" + (ImagePluginConfiguration.getRange(new CategoryProvider(TransformerPluginProvider.SORTER))[0] + 1) + "/true",
                "plugin2_C:" + (ImagePluginConfiguration.getRange(new CategoryProvider(TransformerPluginProvider.COMPRESSOR))[0] + 1) + "/true"), expected, order);
    }

    public static class PProvider extends TransformerCmdProvider {

        private final List<String> order;
        private final String category;
        private final String name;
        PProvider(String name, String category, List<String> order) {
            super(name, "");
            this.name = name;
            this.order = order;
            this.category = category;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getToolOption() {
            return name;
        }

        @Override
        public String getToolArgument() {
            return null;
        }

        @Override
        public Map<String, String> getAdditionalOptions() {
            return null;
        }

        @Override
        public TransformerPlugin newPlugin(String[] arguments, Map<String, String> otherOptions) {
            return new PluginTrap(getName(), order);
        }

        @Override
        public Type getType() {
            return Type.RESOURCE_PLUGIN;
        }
    }

    public static class PluginTrap implements TransformerPlugin {

        private final String name;
        private final List<String> order;

        private PluginTrap(String name, List<String> order) {
            this.name = name;
            this.order = order;
        }

        @Override
        public void visit(Pool resources, Pool output) {
            order.add(name);
            resources.visit((resource) -> {
                return resource;
            }, output);
        }

        @Override
        public String getName() {
            return name;
        }
    }

    public static class CategoryProvider extends TransformerCmdProvider {

        private final String category;

        CategoryProvider(String category) {
            super("CategoryProvider", "");
            this.category = category;
        }

        @Override
        public String getCategory() {
            return category;
        }

        @Override
        public String getToolArgument() {
            throw new UnsupportedOperationException("Shouldn't be called");
        }

        @Override
        public String getToolOption() {
            throw new UnsupportedOperationException("Shouldn't be called");
        }

        @Override
        public Map<String, String> getAdditionalOptions() {
            throw new UnsupportedOperationException("Shouldn't be called");
        }

        @Override
        public TransformerPlugin newPlugin(String[] arguments, Map<String, String> otherOptions) {
            throw new PluginException("Shouldn't be called");
        }

        @Override
        public Type getType() {
            return Type.RESOURCE_PLUGIN;
        }
    }
}
