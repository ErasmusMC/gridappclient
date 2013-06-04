/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package client.model;

/**
 *
 * @author bram
 */
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.table.*;

/**
 * A TableModel that better supports the processing of rows of data. That is,
 * the data is treated more like a row than an individual cell.
 */
public class RowTableModel<T> extends AbstractTableModel {

    private static Map<Class, Class> primitives;

    static {
        Map<Class, Class> map = new HashMap<>(10);
        map.put(Boolean.TYPE, Boolean.class);
        map.put(Byte.TYPE, Byte.class);
        map.put(Character.TYPE, Character.class);
        map.put(Double.TYPE, Double.class);
        map.put(Float.TYPE, Float.class);
        map.put(Integer.TYPE, Integer.class);
        map.put(Long.TYPE, Long.class);
        map.put(Short.TYPE, Short.class);
        primitives = Collections.unmodifiableMap(map);
    }
    protected List<T> modelData = new ArrayList<>();
    private List<ColumnInformation> columns = new ArrayList<>();

    public RowTableModel(Class<?> c, String... getMethodNames) {

        for (String getMethodName : getMethodNames) {
            try {
                Method getMethod = c.getMethod(getMethodName, new Class<?>[]{});
                Class returnType = getReturnType(getMethod);
                String methodName = null;
                if (getMethod.getName().startsWith("get")) {
                    methodName = getMethod.getName().substring(3);
                }

                if (getMethod.getName().startsWith("is")) {
                    methodName = getMethod.getName().substring(2);
                }

                if (returnType == null || methodName == null) {
                    Logger.getLogger(RowTableModel.class.getName()).log(Level.WARNING, "Can''t map {0}!", c.getName() +"." +  getMethod.getName());
                    continue;
                }

                String columnName = formatColumnName(methodName);
                Method setMethod = null; //c.getMethod("set" + methodName, getMethod.getReturnType());

                columns.add(new ColumnInformation(columnName, returnType, getMethod, setMethod));

            } catch (NoSuchMethodException | SecurityException ex) {
                Logger.getLogger(RowTableModel.class.getName()).log(Level.WARNING, "Can't find setMethod!", ex);
            }
        }
    }

    /**
     * Make sure the return type of the method is something we can use.
     */
    private Class getReturnType(Method theMethod) {
        Class returnType = theMethod.getReturnType();

        if (returnType.isInterface() || returnType.isArray()) {
            return null;
        }

        //  The primitive class type is different then the wrapper class of the
        //  primitive. We need the wrapper class.
        if (returnType.isPrimitive()) {
            returnType = primitives.get(returnType);
        }
        return returnType;
    }

    @Override
    public Class getColumnClass(int column) {
        return columns.get(column).getReturnType();
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public String getColumnName(int column) {
        return columns.get(column).getName();
    }

    @Override
    public int getRowCount() {
        return modelData.size();
    }

    @Override
    public boolean isCellEditable(int row, int column) {
        return false;
    }
    
    public List<T> getObjects() {
        return Collections.unmodifiableList(modelData);
    }

    public T getRow(int row) {
        return modelData.get(row);
    }

    public int indexOf(T row) {
        return modelData.indexOf(row);
    }
    
    public boolean contains(T row) {
        return modelData.contains(row);
    }

    public void add(T row) {
        addRow(getRowCount(), row);
    }

    protected void addRow(int index, T row) {
        modelData.add(index, row);
        fireTableRowsInserted(index, index);
    }
    
    public void replace(T row) {
        int index = modelData.indexOf(row);
        if(index >= 0) {
            modelData.set(index, row);
        }
    }

    public void remove(T row) {
        int index = indexOf(row);
        modelData.remove(row);
        fireTableRowsDeleted(index, index);
    }

    protected void removeRow(int index) {
        modelData.remove(index);
        fireTableRowsDeleted(index, index);
    }

    public void removeRows(List<Integer> rows) {
        Collections.sort(rows, Collections.reverseOrder());
        for (int row : rows) {
            removeRow(row);
        }
    }

    @Override
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object object = null;
        try {
            object = columns.get(columnIndex).getGetter().invoke(getRow(rowIndex));
        } catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException ex) {
            Logger.getLogger(RowTableModel.class.getName()).log(Level.SEVERE, ex.getMessage(), ex);
        }
        return object;
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    }

    /**
     * Class to hold data required to implement the TableModel interface.
     */
    private class ColumnInformation implements Comparable<ColumnInformation> {

        private String name;
        private Class returnType;
        private Method getter;
        private Method setter;

        public ColumnInformation(String name, Class returnType, Method getter, Method setter) {
            this.name = name;
            this.returnType = returnType;
            this.getter = getter;
            this.setter = setter;
        }

        /**
         * The column class of the model.
         */
        public Class getReturnType() {
            return returnType;
        }

        /**
         * Used by the getValueAt() method to get the data for the cell.
         */
        public Method getGetter() {
            return getter;
        }

        /**
         * The value used as the column header name.
         */
        public String getName() {
            return name;
        }

        /**
         * Used by the setValueAt() method to update the bean.
         */
        public Method getSetter() {
            return setter;
        }

        /**
         * Use to change the column header name.
         */
        public void setName(String name) {
            this.name = name;
        }

        /**
         * Implement the natural sort order for this class.
         */
        @Override
        public int compareTo(ColumnInformation o) {
            return getName().compareTo(o.getName());
        }
    }

    /*
     *  Convert an unformatted column name to a formatted column name. That is:
     *
     *  - insert a space when a new uppercase character is found, insert
     *    multiple upper case characters are grouped together.
     *  - replace any "_" with a space
     *
     *  @param columnName  unformatted column name
     *  @return the formatted column name
     */
    public static String formatColumnName(String columnName) {
        if (columnName.length() < 3) {
            return columnName;
        }

        StringBuilder buffer = new StringBuilder(columnName);
        boolean isPreviousLowerCase = false;

        for (int i = 1; i < buffer.length(); i++) {
            boolean isCurrentUpperCase = Character.isUpperCase(buffer.charAt(i));

            if (isCurrentUpperCase && isPreviousLowerCase) {
                buffer.insert(i, " ");
                i++;
            }
            isPreviousLowerCase = !isCurrentUpperCase;
        }
        return buffer.toString().replaceAll("_", " ");
    }
}
