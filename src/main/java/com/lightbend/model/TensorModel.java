package com.lightbend.model;

/**
 * Created by boris on 5/26/17.
 */

import org.tensorflow.Graph;
import org.tensorflow.Session;
import org.tensorflow.Tensor;

public class TensorModel implements Model{
    private Graph graph = new Graph();
    private Session session;

    public TensorModel(byte[] inputStream) {
        graph.importGraphDef(inputStream);
        session = new Session(graph);
    }

    @Override
    public Object score(Object input) {
        Winerecord.WineRecord record = (Winerecord.WineRecord) input;
        float[][] data = {{
                (float)record.getFixedAcidity(),
                (float)record.getVolatileAcidity(),
                (float)record.getCitricAcid(),
                (float)record.getResidualSugar(),
                (float)record.getChlorides(),
                (float)record.getFreeSulfurDioxide(),
                (float)record.getTotalSulfurDioxide(),
                (float)record.getDensity(),
                (float)record.getPH(),
                (float)record.getSulphates(),
                (float)record.getAlcohol()
        }};
        Tensor modelInput = Tensor.create(data);
        Tensor result = session.runner().feed("dense_1_input", modelInput).fetch("dense_3/Sigmoid").run().get(0);
        long[] rshape = result.shape();
        float[][] rMatrix = new float[(int)rshape[0]][(int)rshape[1]];
        result.copyTo(rMatrix);
        Intermediate value = new Intermediate(0, rMatrix[0][0]);
        for(int i=1; i < rshape[1]; i++){
            if(rMatrix[0][i] > value.getValue()) {
                value.setIndex(i);
                value.setValue(rMatrix[0][i]);
            }
        }
        return (double)value.getIndex();
    }

    @Override
    public void cleanup() {
        session.close();
        graph.close();
    }

    public Graph getGraph() {
        return graph;
    }

    private class Intermediate{
        private int index;
        private float value;
        public Intermediate(int i, float v){
            index = i;
            value = v;
        }

        public int getIndex() {
            return index;
        }

        public void setIndex(int index) {
            this.index = index;
        }

        public float getValue() {
            return value;
        }

        public void setValue(float value) {
            this.value = value;
        }
    }
}
