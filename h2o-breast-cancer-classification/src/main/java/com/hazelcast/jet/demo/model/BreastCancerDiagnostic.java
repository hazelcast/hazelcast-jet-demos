package com.hazelcast.jet.demo.model;

import hex.genmodel.easy.RowData;

public class BreastCancerDiagnostic {
    private long id;
    private TumorType diagnosis;
    private double radiusMean;
    private double textureMean;
    private double perimeterMean;
    private double areaMean;
    private double smoothnessMean;
    private double compactnessMean;
    private double concavityMean;
    private double concavePointsMean;
    private double symmetryMean;
    private double fractalDimensionMean;
    private double radiusSe;
    private double textureSe;
    private double perimeterSe;
    private double areaSe;
    private double smoothnessSe;
    private double compactnessSe;
    private double concavitySe;
    private double concavePointsSe;
    private double symmetrySe;
    private double fractalDimensionSe;
    private double radiusWorst;
    private double textureWorst;
    private double perimeterWorst;
    private double areaWorst;
    private double smoothnessWorst;
    private double compactnessWorst;
    private double concavityWorst;
    private double concavePointsWorst;
    private double symmetryWorst;
    private double fractalDimensionWorst;

    public BreastCancerDiagnostic(String line) {
        String[] split = line.split(",");
        this.id = Long.parseLong(split[0]);
        this.diagnosis = TumorType.fromString(split[1].replace('"',' ').trim());
        this.radiusMean = Double.parseDouble(split[2]);
        this.textureMean = Double.parseDouble(split[3]);
        this.perimeterMean = Double.parseDouble(split[4]);
        this.areaMean = Double.parseDouble(split[5]);
        this.smoothnessMean = Double.parseDouble(split[6]);
        this.compactnessMean = Double.parseDouble(split[7]);
        this.concavityMean = Double.parseDouble(split[8]);
        this.concavePointsMean = Double.parseDouble(split[9]);
        this.symmetryMean = Double.parseDouble(split[10]);
        this.fractalDimensionMean = Double.parseDouble(split[11]);
        this.radiusSe = Double.parseDouble(split[12]);
        this.textureSe = Double.parseDouble(split[13]);
        this.perimeterSe = Double.parseDouble(split[14]);
        this.areaSe = Double.parseDouble(split[15]);
        this.smoothnessSe = Double.parseDouble(split[16]);
        this.compactnessSe = Double.parseDouble(split[17]);
        this.concavitySe = Double.parseDouble(split[18]);
        this.concavePointsSe = Double.parseDouble(split[19]);
        this.symmetrySe = Double.parseDouble(split[20]);
        this.fractalDimensionSe = Double.parseDouble(split[21]);
        this.radiusWorst = Double.parseDouble(split[22]);
        this.textureWorst = Double.parseDouble(split[23]);
        this.perimeterWorst = Double.parseDouble(split[24]);
        this.areaWorst = Double.parseDouble(split[25]);
        this.smoothnessWorst = Double.parseDouble(split[26]);
        this.compactnessWorst = Double.parseDouble(split[27]);
        this.concavityWorst = Double.parseDouble(split[28]);
        this.concavePointsWorst = Double.parseDouble(split[29]);
        this.symmetryWorst = Double.parseDouble(split[30]);
        this.fractalDimensionWorst = Double.parseDouble(split[31]);
    }

    public long getId() {
        return id;
    }

    public TumorType getDiagnosis() {
        return diagnosis;
    }

    public double getRadiusMean() {
        return radiusMean;
    }

    public double getTextureMean() {
        return textureMean;
    }

    public double getPerimeterMean() {
        return perimeterMean;
    }

    public double getAreaMean() {
        return areaMean;
    }

    public double getSmoothnessMean() {
        return smoothnessMean;
    }

    public double getCompactnessMean() {
        return compactnessMean;
    }

    public double getConcavityMean() {
        return concavityMean;
    }

    public double getConcavePointsMean() {
        return concavePointsMean;
    }

    public double getSymmetryMean() {
        return symmetryMean;
    }

    public double getFractalDimensionMean() {
        return fractalDimensionMean;
    }

    public double getRadiusSe() {
        return radiusSe;
    }

    public double getTextureSe() {
        return textureSe;
    }

    public double getPerimeterSe() {
        return perimeterSe;
    }

    public double getAreaSe() {
        return areaSe;
    }

    public double getSmoothnessSe() {
        return smoothnessSe;
    }

    public double getCompactnessSe() {
        return compactnessSe;
    }

    public double getConcavitySe() {
        return concavitySe;
    }

    public double getConcavePointsSe() {
        return concavePointsSe;
    }

    public double getSymmetrySe() {
        return symmetrySe;
    }

    public double getFractalDimensionSe() {
        return fractalDimensionSe;
    }

    public double getRadiusWorst() {
        return radiusWorst;
    }

    public double getTextureWorst() {
        return textureWorst;
    }

    public double getPerimeterWorst() {
        return perimeterWorst;
    }

    public double getAreaWorst() {
        return areaWorst;
    }

    public double getSmoothnessWorst() {
        return smoothnessWorst;
    }

    public double getCompactnessWorst() {
        return compactnessWorst;
    }

    public double getConcavityWorst() {
        return concavityWorst;
    }

    public double getConcavePointsWorst() {
        return concavePointsWorst;
    }

    public double getSymmetryWorst() {
        return symmetryWorst;
    }

    public double getFractalDimensionWorst() {
        return fractalDimensionWorst;
    }

    public RowData asRow() {
        RowData row = new RowData();
        row.put("radius_mean", this.radiusMean);
        row.put("texture_mean", this.textureMean);
        row.put("perimeter_mean", this.perimeterMean);
        row.put("area_mean", this.areaMean);
        row.put("smoothness_mean", this.smoothnessMean);
        row.put("compactness_mean", this.compactnessMean);
        row.put("concavity_mean", this.concavityMean);
        row.put("concave_points_mean", this.concavePointsMean);
        row.put("symmetry_mean", this.symmetryMean);
        row.put("fractal_dimension_mean", this.fractalDimensionMean);
        row.put("radius_se", this.radiusSe);
        row.put("texture_se", this.textureSe);
        row.put("perimeter_se", this.perimeterSe);
        row.put("area_se", this.areaSe);
        row.put("smoothness_se", this.smoothnessSe);
        row.put("compactness_se", this.compactnessSe);
        row.put("concavity_se", this.concavitySe);
        row.put("concave_points_se", this.concavePointsSe);
        row.put("symmetry_se", this.symmetrySe);
        row.put("fractal_dimension_se", this.fractalDimensionSe);
        row.put("radius_worst", this.radiusWorst);
        row.put("texture_worst", this.textureWorst);
        row.put("perimeter_worst", this.perimeterWorst);
        row.put("area_worst", this.areaWorst);
        row.put("smoothness_worst", this.smoothnessWorst);
        row.put("compactness_worst", this.compactnessWorst);
        row.put("concavity_worst", this.concavityWorst);
        row.put("concave_points_worst", this.concavePointsWorst);
        row.put("symmetry_worst", this.symmetryWorst);
        row.put("fractal_dimension_worst", this.fractalDimensionWorst);
        return row;
    }

    @Override
    public String toString() {
        return "BreastCancerDiagnostic{" +
                "id=" + id +
                ", diagnosis='" + diagnosis + '\'' +
                ", radiusMean=" + radiusMean +
                ", textureMean=" + textureMean +
                ", perimeterMean=" + perimeterMean +
                ", areaMean=" + areaMean +
                ", smoothnessMean=" + smoothnessMean +
                ", compactnessMean=" + compactnessMean +
                ", concavityMean=" + concavityMean +
                ", concavePointsMean=" + concavePointsMean +
                ", symmetryMean=" + symmetryMean +
                ", fractalDimensionMean=" + fractalDimensionMean +
                ", radiusSe=" + radiusSe +
                ", textureSe=" + textureSe +
                ", perimeterSe=" + perimeterSe +
                ", areaSe=" + areaSe +
                ", smoothnessSe=" + smoothnessSe +
                ", compactnessSe=" + compactnessSe +
                ", concavitySe=" + concavitySe +
                ", concavePointsSe=" + concavePointsSe +
                ", symmetrySe=" + symmetrySe +
                ", fractalDimensionSe=" + fractalDimensionSe +
                ", radiusWorst=" + radiusWorst +
                ", textureWorst=" + textureWorst +
                ", perimeterWorst=" + perimeterWorst +
                ", areaWorst=" + areaWorst +
                ", smoothnessWorst=" + smoothnessWorst +
                ", compactnessWorst=" + compactnessWorst +
                ", concavityWorst=" + concavityWorst +
                ", concavePointsWorst=" + concavePointsWorst +
                ", symmetryWorst=" + symmetryWorst +
                ", fractalDimensionWorst=" + fractalDimensionWorst +
                '}';
    }
}
