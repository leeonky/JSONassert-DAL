package com.github.leeonky.dal.extension.jsonassert;

import com.github.leeonky.dal.DAL;
import com.github.leeonky.dal.ast.AssertionFailure;
import com.github.leeonky.dal.runtime.ArrayAccessor;
import com.github.leeonky.dal.runtime.DalException;
import com.github.leeonky.dal.runtime.PropertyAccessor;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.skyscreamer.jsonassert.JSONCompareMode;
import org.skyscreamer.jsonassert.JSONCompareResult;
import org.skyscreamer.jsonassert.comparator.DefaultComparator;

import java.util.Iterator;
import java.util.Set;
import java.util.Spliterator;
import java.util.stream.Collectors;

import static java.util.Spliterators.spliteratorUnknownSize;
import static java.util.stream.StreamSupport.stream;

public class PatternComparator extends DefaultComparator {
    public static final String PREFIX = "**";
    private final String prefix;
    private DAL dal = DAL.getInstance();

    private PatternComparator(String prefix) {
        super(JSONCompareMode.STRICT);
        this.prefix = prefix;
        dal.getRuntimeContextBuilder().registerPropertyAccessor(JSONObject.class, new PropertyAccessor<JSONObject>() {
            @Override
            public Object getValue(JSONObject instance, String name) {
                try {
                    return instance.has(name) ? instance.get(name) : JSONObject.NULL;
                } catch (JSONException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            @Override
            @SuppressWarnings("unchecked")
            public Set<String> getPropertyNames(JSONObject instance) {
                return stream(spliteratorUnknownSize((Iterator<String>) instance.keys(), Spliterator.NONNULL), false)
                        .collect(Collectors.toSet());
            }

            @Override
            public boolean isNull(JSONObject instance) {
                return instance == null || instance.equals(JSONObject.NULL);
            }
        });

        dal.getRuntimeContextBuilder().registerListAccessor(JSONArray.class, new ArrayAccessor<JSONArray>() {
            @Override
            public Object get(JSONArray jsonArray, int index) {
                try {
                    return jsonArray.get(index);
                } catch (JSONException e) {
                    throw new IllegalArgumentException(e);
                }
            }

            @Override
            public int size(JSONArray jsonArray) {
                return jsonArray.length();
            }
        });
    }

    public static PatternComparator defaultPatternComparator() {
        return new PatternComparator(PREFIX);
    }

    public DAL getDal() {
        return dal;
    }

    public void setDal(DAL dal) {
        this.dal = dal;
    }

    @Override
    public void compareValues(String prefix, Object expectedValue, Object actualValue, JSONCompareResult result)
            throws JSONException {
        if (expectedValue instanceof String) {
            String codeWithPrefix = ((String) expectedValue).trim();
            if (codeWithPrefix.startsWith(this.prefix)) {
                String code = codeWithPrefix.substring(this.prefix.length()).trim();
                try {
                    dal.evaluateAll(actualValue, code);
                } catch (AssertionFailure e) {
                    result.fail(prefix, expectedValue, String.valueOf(actualValue));
                    result.fail("\n" + e.show(code) + "\n" + e.getMessage());
                } catch (DalException e) {
                    throw new RuntimeException(e.getClass().getSimpleName() + ": " + e.getMessage() + "\n" + e.show(code));
                }
                return;
            }
        }
        super.compareValues(prefix, expectedValue, actualValue, result);
    }
}
