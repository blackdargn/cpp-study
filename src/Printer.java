package com.example.protocol;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Collection;

public class Printer {
    public void dump() {
        if (true) {
            StringBuffer sb = new StringBuffer();
            dump(0, sb);
            System.out.print(sb.toString());
        }
    }

    public void dump(int depth, StringBuffer sb) {
        Class c = this.getClass();
        if (sb == null) {
            sb = new StringBuffer();
        }

        int currentDepth = depth;
        addTab(currentDepth, sb);
        sb.append(c.getSimpleName());
        sb.append(" begin");
        sb.append("\n");

        dumpFields(++depth, c, sb);

        addTab(currentDepth, sb);
        sb.append(c.getSimpleName());
        sb.append(" end");
    }

    private void dumpFields(int depth, Class c, StringBuffer sb) {
        if (c == null) {
            return;
        }

        Class parent = c.getSuperclass();
        if (parent != null) {
            dumpFields(depth, parent, sb);
        }

        Field[] fields = c.getDeclaredFields();

        for (Field field : fields) {
            if (Modifier.isStatic(field.getModifiers())) {
                continue;
            }

            boolean visible = field.isAccessible();

            field.setAccessible(true);
            try {
                Object o = field.get(this);
                addTab(depth, sb);
                sb.append(field.getName());
                sb.append(":");
                if (o != null) {
                    if (o instanceof Printer) {
                        sb.append("\n");
                        ((Printer) o).dump(++depth, sb);
                    } else {
                        if (o instanceof Collection) {
                            Collection list = (Collection) o;
                            sb.append("\n-------------- " + field.getName() + " Begin --------------\n");
                            depth++;
                            int index = 0;
                            for (Object e : list) {
                                if (e instanceof Printer) {
                                    sb.append(++index + ".\n");
                                    ((Printer) e).dump(depth, sb);
                                } else {
                                    sb.append(e.toString() + "\n");
                                }
                            }
                            sb.append("\n-------------- " + field.getName() + " End --------------\n");
                        } else if (!isArray(o, sb)) {
                            sb.append(o.toString());
                        }
                    }
                }
                sb.append("\n");
            } catch (Exception e) {
                e.printStackTrace();
            }

            field.setAccessible(visible);
        }
    }

    private boolean isArray(Object o, StringBuffer sb) {
        if (o instanceof short[]) {
            short[] arr = (short[]) o;
            StringBuffer temp = new StringBuffer();
            if (arr != null) {
                for (short s : arr) {
                    temp.append(s + " ");
                }

                sb.append(temp);
            }
            return true;
        } else if (o instanceof byte[]) {
            byte[] arr = (byte[]) o;
            StringBuffer temp = new StringBuffer();
            if (arr != null) {
                for (byte s : arr) {
                    temp.append(s + " ");
                }

                sb.append(temp);
            }
            return true;
        } else if (o instanceof int[]) {
            int[] arr = (int[]) o;
            StringBuffer temp = new StringBuffer();
            if (arr != null) {
                for (int s : arr) {
                    temp.append(s + " ");
                }

                sb.append(temp);
            }
            return true;
        } else if (o instanceof long[]) {
            long[] arr = (long[]) o;
            StringBuffer temp = new StringBuffer();
            if (arr != null) {
                for (long s : arr) {
                    temp.append(s + " ");
                }

                sb.append(temp);
            }
            return true;
        } else if (o instanceof float[]) {
            float[] arr = (float[]) o;
            StringBuffer temp = new StringBuffer();
            if (arr != null) {
                for (float s : arr) {
                    temp.append(s + " ");
                }

                sb.append(temp);
            }
            return true;
        } else if (o instanceof double[]) {
            double[] arr = (double[]) o;
            StringBuffer temp = new StringBuffer();
            if (arr != null) {
                for (double s : arr) {
                    temp.append(s + " ");
                }

                sb.append(temp);
            }
            return true;
        }
        return false;
    }

    private void addTab(int depth, StringBuffer sb) {
        for (int i = 0; i < depth; i++) {
            sb.append("    ");
        }
    }
}
