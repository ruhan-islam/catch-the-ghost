package sample;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Rectangle;

import java.net.URL;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class Controller {
    private static final ArrayList<Particle> particleList =  new ArrayList<>();
    private static final ArrayList<Particle> tempList =  new ArrayList<>();

    private static final int far = 5;
    private static final int near = 2;
    private static final int nStart = 200;

    private static final double multiplicationFactor = 100.0;
    private static final double probZero = 0.0001;
    private static final double probOne = 1.0 - probZero;
    private static final double probTH = 0.001;

    private static final int [] incRow =   {0, -1, +1, -1, +1,  0, -1, +1,  0};
    private static final int [] incCol =   {0, -1, -1, +1, +1, -1,  0,  0, +1};

    private static final double [] pdmc = {0, 4, 10, 16, 22, 28, 46, 64, 82, 100};
    private static final double [] pdec = {0, 5, 15, 25, 50, 75, 100};
    private static final double [] pdcc = {0, 6, 20, 60, 100};

    private static final double pM2O = pdmc[1] - pdmc[0];
    private static final double pM2C = pdmc[2] - pdmc[1];
    private static final double pM2S = pdmc[9] - pdmc[8];

    private static final double pC2O = pdcc[1] - pdcc[0];
    private static final double pC2C = pdcc[2] - pdcc[1];
    private static final double pC2S = pdcc[4] - pdcc[3];

    private static final double pE2O = pdec[1] - pdec[0];
    private static final double pE2C = pdec[2] - pdec[1];
    private static final double pE2S = pdec[6] - pdec[5];

    private static int nParticles, timeCount, senseCount, catchCount, nRow, nCol, gRow, gCol;
    private static double cellWidth, cellHeight;
    private static boolean probMode;

    private static int [][] pCount;
    private static double [][] probs, tempProbs;

    private static Paint [][] cellColors;
    private static Label[][] labels;
    private static Rectangle[][] recs;


    @FXML // ResourceBundle that was given to the FXMLLoader
    private ResourceBundle resources;

    @FXML // URL location of the FXML file that was given to the FXMLLoader
    private URL location;

    @FXML // fx:id="restartButton"
    private Button restartButton; // Value injected by FXMLLoader

    @FXML // fx:id="catchButton"
    private Button catchButton; // Value injected by FXMLLoader

    @FXML // fx:id="senseButton"
    private Button senseButton; // Value injected by FXMLLoader

    @FXML // fx:id="advanceTimeButton"
    private Button advanceTimeButton; // Value injected by FXMLLoader

    @FXML // fx:id="showButton"
    private Button showButton; // Value injected by FXMLLoader

    @FXML // fx:id="modeButton"
    private Button modeButton; // Value injected by FXMLLoader

    @FXML // fx:id="grid"
    private GridPane grid; // Value injected by FXMLLoader

    @FXML // fx:id="senseCountLabel"
    private Label senseCountLabel; // Value injected by FXMLLoader

    @FXML // fx:id="catchCountLabel"
    private Label catchCountLabel; // Value injected by FXMLLoader

    @FXML // fx:id="timeCountLabel"
    private Label timeCountLabel; // Value injected by FXMLLoader

    @FXML // fx:id="resultLabel"
    private Label resultLabel; // Value injected by FXMLLoader


    @FXML // This method is called by the FXMLLoader when initialization is complete
    void initialize() {
        assert catchButton != null : "fx:id=\"catchButton\" was not injected: check your FXML file 'sample.fxml'.";
        assert senseButton != null : "fx:id=\"senseButton\" was not injected: check your FXML file 'sample.fxml'.";
        assert advanceTimeButton != null : "fx:id=\"advanceTimeButton\" was not injected: check your FXML file 'sample.fxml'.";
        assert grid != null : "fx:id=\"grid\" was not injected: check your FXML file 'sample.fxml'.";

//        nRow = grid.getRowCount();
        nRow = 9;
//        nCol = grid.getColumnCount();
        nCol = 9;
        cellWidth = grid.getPrefWidth() / nCol;
        cellHeight = grid.getPrefHeight() / nRow;

        pCount = new int[nRow][nCol];
        probs = new double[nRow][nCol];
        tempProbs = new double[nRow][nCol];

        cellColors = new Paint[nRow][nCol];
        labels = new Label[nRow][nCol];
        recs = new Rectangle[nRow][nCol];

        for (int i = 0; i < nRow; i++) {
            for (int j = 0; j < nCol; j++) {
                recs[i][j] = new Rectangle();
                recs[i][j].setWidth(cellWidth);
                recs[i][j].setHeight(cellHeight);
                grid.add(recs[i][j], j, i); // add(node, colIdx, rowIdx);

                labels[i][j] = new Label();
                grid.add(labels[i][j], j, i); // add(node, colIdx, rowIdx);
            }
        }

        init();
    }

    private void init() {
        grid.setDisable(false);
        grid.setGridLinesVisible(true);
        senseButton.setDisable(true);
        catchButton.setDisable(false);
        resultLabel.setVisible(false);
        advanceTimeButton.setDisable(false);
        showButton.setDisable(false);
        modeButton.setDisable(false);

        probMode = true;
        modeButton.setText(" Particle  View ");

        timeCount = 0;
        timeCountLabel.setText("" + timeCount);
        senseCount = 0;
        senseCountLabel.setText("" + senseCount);
        catchCount = 0;
        catchCountLabel.setText("" + catchCount);

        gRow = (int) (Math.random() * nRow);
        gCol = (int) (Math.random() * nCol);

        for (int i = 0; i < nRow; i++) {
            for (int j = 0; j < nCol; j++) {
                probs[i][j] = 1.0 / (nRow * nCol);
                pCount[i][j] = 0;

                recs[i][j].setOpacity(0.5);
                recs[i][j].setFill(Color.WHITE);

                labels[i][j].setText(" " + String.format("%.4f", (probs[i][j] * multiplicationFactor)));
                labels[i][j].setOpacity(1.0);
            }
        }

        //--------------------------------------------------------------------------------------------------------------
        nParticles = nStart;
        tempList.clear();
        particleList.clear();
        for (int i = 0; i < nParticles; i++) {
            int rr, cc, rc = (int) (Math.random() * nRow * nCol);
            rr = rc / nRow;
            cc = rc % nCol;
            particleList.add(new Particle(rr, cc));
            pCount[rr][cc]++;
        }
    }

    private void catchGhost(int row, int col) {
        catchCount++;
        catchCountLabel.setText("" + catchCount);

        if (row == gRow && col == gCol) {
            for (int i = 0; i < nRow; i++) {
                for (int j = 0; j < nCol; j++) {
                    probs[i][j] = 0.0;
                    pCount[i][j] = 0;
                }
            }

            probs[gRow][gCol] = 1.0;
            pCount[gRow][gCol] = nParticles;

            for (int ii = 0; ii < nParticles; ii++) {
                particleList.get(ii).row = gRow;
                particleList.get(ii).col = gCol;
            }

            recs[gRow][gCol].setFill(Color.BLUE);
            resultLabel.setVisible(true);
            grid.setDisable(true);
            senseButton.setDisable(true);
            catchButton.setDisable(true);
            advanceTimeButton.setDisable(true);
        }
        else { // catch failed
            double tp = 1.0 - probs[row][col];
            probs[row][col] = 0.0;
            for (int i = 0; i < nRow; i++) {
                for (int j = 0; j < nCol; j++) {
                    probs[i][j] /= tp;
                }
            }

            //----------------------------------------------------------------------------------------------------------
            // TODO
            int removed = 0, remaining = 0;
            for (int ii = 0; ii < nParticles; ii++) {
                Particle particle = particleList.get(ii);
                if (particle.row == row && particle.col == col) {
                    pCount[row][col]--;
                    tempList.add(particleList.remove(ii--));
                    nParticles--;
                    removed++;
                } else {
                    remaining++;
                }
            }

            if (remaining <= 0) {
                System.err.println("no particle!!");
                remaining = 1;
            }
            double factor = 1.0 * removed / remaining;
            resample(factor);
        }
    }

    private void resample(double factor) {
        int stored = tempList.size();
        for (int i = 0; i < nRow; i++) {
            for (int j = 0; j < nCol; j++) {
                int count = (int) (factor * pCount[i][j] + 0.5);
                for (int k = 0; k < count; k++) {
                    if (stored > 0) {
                        Particle particle = tempList.remove(0);
                        stored--;
                        particle.row = i;
                        particle.col = j;
                        particleList.add(particle);
                    }
                    else {
                        particleList.add(new Particle(i, j));
                    }
                }
                pCount[i][j] += count;
                nParticles += count;
            }
        }
    }

    private void senseGhost(int row, int col) {
        senseCount++;
        senseCountLabel.setText("" + senseCount);

        int minDist, maxDist, dist = Math.abs(gRow - row) + Math.abs(gCol - col);

        if (dist <= near) {
            minDist = 0;
            maxDist = near;
            recs[row][col].setFill(Color.RED);
        } else if (dist >= far) {
            minDist = far;
            maxDist = nRow * nCol;
            recs[row][col].setFill(Color.GREEN);
        } else {
            minDist = near + 1;
            maxDist = far - 1;
            recs[row][col].setFill(Color.ORANGE);
        }

        double cellProb, totalProb = 0.0;
        for (int i = 0; i < nRow; i++) {
            for (int j = 0; j < nCol; j++) {
                dist = Math.abs(i - row) + Math.abs(j - col);
                if (dist >= minDist && dist <= maxDist) {
                    cellProb = probOne;
                } else {
                    cellProb = probZero;
                }
                probs[i][j] *= cellProb;
                totalProb += probs[i][j];
            }
        }
        for (int i = 0; i < nRow; i++) {
            for (int j = 0; j < nCol; j++) {
                probs[i][j] /= totalProb;
            }
        }

        //--------------------------------------------------------------------------------------------------------------
        int removed = 0, remaining = 0;

        for (int ii = 0; ii < nParticles;  ii++) {
            Particle particle = particleList.get(ii);
            dist = Math.abs(particle.row - row) + Math.abs(particle.col - col);
            if (dist >= minDist && dist <= maxDist) {
                remaining++;
            } else {
                pCount[particle.row][particle.col]--;
                tempList.add(particleList.remove(ii--));
                nParticles--;
                removed++;
            }
        }

        // TODO resampling
        if (remaining <= 0) {
            System.err.println("no particle!!");
            remaining = 1;
        }
        double factor = 1.0 * removed / remaining;
        resample(factor);
    }

    private void moveGhost() {
        double rand = Math.random() * 100;

        if ((gRow > 0) && (gRow < nRow-1) && (gCol > 0) && (gCol < nCol-1)) { // middle cell
            for (int i = 0; i < 9; i++) {
                if (rand < pdmc[i+1]) {
                    gRow += incRow[i];
                    gCol += incCol[i];
                    break;
                }
            }
        }
        else if (((gRow == 0) || (gRow == nRow-1)) && ((gCol == 0) || (gCol == nCol-1))) { // corner cell
            for (int i = 0, j = 0; i < 9; i++) {
                int tRow = gRow + incRow[i], tCol = gCol + incCol[i];
                if (tRow >= 0 && tRow < nRow && tCol >= 0 && tCol < nCol) {
                    j++;
                    if (rand < pdcc[j]) {
                        gRow = tRow;
                        gCol = tCol;
                        break;
                    }
                }
            }
        }
        else { // edge cell
            for (int i = 0, j = 0; i < 9; i++) {
                int tRow = gRow + incRow[i], tCol = gCol + incCol[i];
                if (tRow >= 0 && tRow < nRow && tCol >= 0 && tCol < nCol) {
                    j++;
                    if (rand < pdec[j]) {
                        gRow = tRow;
                        gCol = tCol;
                        break;
                    }
                }
            }
        }
    }

    private void updateCells() {
        for (int i = 0; i < nRow; i++) {
            if (nCol >= 0) System.arraycopy(probs[i], 0, tempProbs[i], 0, nCol);
        }

        for (int rr  = 0; rr < nRow; rr++) {
            for (int cc = 0; cc < nCol; cc++) {
                recs[rr][cc].setFill(Color.WHITE);
                double prob = 0.0;

                for (int i = 0; i < 9; i++) {
                    int tRow = rr;
                    int tCol = cc;

                    tRow += incRow[i];
                    tCol += incCol[i];

                    if (tRow < 0 || tRow == nRow || tCol < 0|| tCol == nCol) {
                        continue;
                    }

                    int moveType = Math.abs(tRow - rr) + Math.abs(tCol - cc);
                    switch (moveType) {
                        case 0 : { // no chng
                            if ((tRow > 0) && (tRow < nRow-1) && (tCol > 0) && (tCol < nCol-1)) { // from middle cell
                                prob += tempProbs[tRow][tCol] * pM2O / 100.0;
                            } else if (((tRow == 0) || (tRow == nRow-1)) && ((tCol == 0) || (tCol == nCol-1))) { // from corner cell
                                prob += tempProbs[tRow][tCol] * pC2O / 100.0;
                            } else { // from edge cell
                                prob += tempProbs[tRow][tCol] * pE2O / 100.0;
                            }
                            break;
                        }
                        case 1 : { // side directed move
                            if ((tRow > 0) && (tRow < nRow-1) && (tCol > 0) && (tCol < nCol-1)) { // from middle cell
                                prob += tempProbs[tRow][tCol] * pM2S / 100.0;
                            } else if (((tRow == 0) || (tRow == nRow-1)) && ((tCol == 0) || (tCol == nCol-1))) { // from corner cell
                                prob += tempProbs[tRow][tCol] * pC2S / 100.0;
                            } else { // from edge cell
                                prob += tempProbs[tRow][tCol] * pE2S / 100.0;
                            }
                            break;
                        }
                        case 2 : { // corner directed move
                            if ((tRow > 0) && (tRow < nRow-1) && (tCol > 0) && (tCol < nCol-1)) { // from middle cell
                                prob += tempProbs[tRow][tCol] * pM2C / 100.0;
                            } else if (((tRow == 0) || (tRow == nRow-1)) && ((tCol == 0) || (tCol == nCol-1))) { // from corner cell
                                prob += tempProbs[tRow][tCol] * pC2C / 100.0;
                            } else { // from edge cell
                                prob += tempProbs[tRow][tCol] * pE2C / 100.0;
                            }
                            break;
                        }
                        default: {
                            System.err.println("error in switch!");
                        }
                    }
                }

                probs[rr][cc] = prob;
            }
        }
    }

    private void updateParticles() {
        for (int ii = 0; ii < nParticles; ii++) {
            double rand = Math.random() * 100;
            Particle particle = particleList.get(ii);
            int pRow = particle.row;
            int pCol = particle.col;
            pCount[pRow][pCol]--;

            if ((pRow > 0) && (pRow < nRow-1) && (pCol > 0) && (pCol < nCol-1)) { // middle cell
                for (int i = 0; i < 9; i++) {
                    if (rand < pdmc[i+1]) {
                        pRow += incRow[i];
                        pCol += incCol[i];
                        break;
                    }
                }
            }
            else if (((pRow == 0) || (pRow == nRow-1)) && ((pCol == 0) || (pCol == nCol-1))) { // corner cell
                for (int i = 0, j = 0; i < 9; i++) {
                    int tRow = pRow + incRow[i], tCol = pCol + incCol[i];
                    if (tRow >= 0 && tRow < nRow && tCol >= 0 && tCol < nCol) {
                        j++;
                        if (rand < pdcc[j]) {
                            pRow = tRow;
                            pCol = tCol;
                            break;
                        }
                    }
                }
            } else { // edge cell
                for (int i = 0, j = 0; i < 9; i++) {
                    int tRow = pRow + incRow[i], tCol = pCol + incCol[i];
                    if (tRow >= 0 && tRow < nRow && tCol >= 0 && tCol < nCol) {
                        j++;
                        if (rand < pdec[j]) {
                            pRow = tRow;
                            pCol = tCol;
                            break;
                        }
                    }
                }
            }

            particle.row = pRow;
            particle.col = pCol;
            pCount[pRow][pCol]++;
        }
    }

    private void showProbs() {
        for (int i = 0; i < nRow; i++) {
            for (int j = 0; j < nCol; j++) {
                labels[i][j].setText(" " + String.format("%.4f", (probs[i][j] * multiplicationFactor)));
                if (probs[i][j] <= probTH) {
                    labels[i][j].setOpacity(0.5);
                } else {
                    labels[i][j].setOpacity(1.0);
                }
            }
        }
    }

    private void showParticles() {
        for (int i = 0; i < nRow; i++) {
            for (int j = 0; j < nCol; j++) {
                double pp = 1.0 * pCount[i][j] / nParticles;
                labels[i][j].setText(" " + String.format("%.4f", (pp * multiplicationFactor)));
                if (pp <= probTH) {
                    labels[i][j].setOpacity(0.5);
                } else {
                    labels[i][j].setOpacity(1.0);
                }
            }
        }
    }

    @FXML
    public void onMouseClicked(MouseEvent mouseEvent) {
        int col = (int) (mouseEvent.getX() / cellWidth);
        int row = (int) (mouseEvent.getY() / cellHeight);

        if (senseButton.isDisabled()) {
            senseGhost(row, col);
        } else if (catchButton.isDisabled()) {
            catchGhost(row, col);
        } else {
            System.err.println(" error in mouse click event");
        }

        if (probMode) {
            showProbs();
        } else {
            showParticles();
        }
    }

    @FXML
    public void advanceTimeClicked(MouseEvent mouseEvent) {
        timeCount++;
        timeCountLabel.setText("" + timeCount);

        moveGhost();
        updateCells();
        updateParticles();

        if (probMode) {
            showProbs();
        } else {
            showParticles();
        }
    }

    @FXML
    public void senseButtonClicked(MouseEvent mouseEvent) {
        catchButton.setDisable(false);
        senseButton.setDisable(true);
    }

    @FXML
    public void catchButtonClicked(MouseEvent mouseEvent) {
        senseButton.setDisable(false);
        catchButton.setDisable(true);
    }

    @FXML
    public void modeButtonClicked(MouseEvent mouseEvent) {
        if (probMode) {
            probMode = false;
            modeButton.setText("Probability View");
            showParticles();
        } else {
            probMode = true;
            modeButton.setText(" Particle  View ");
            showProbs();
        }
    }

    @FXML
    public void showGhostEntered(MouseEvent mouseEvent) {
        for (int row = 0; row < nRow; row++) {
            for (int col = 0; col < nCol; col++) {
                cellColors[row][col] = recs[row][col].getFill();
                int dist = Math.abs(gRow - row) + Math.abs(gCol - col);
                if (dist <= near) {
                    recs[row][col].setFill(Color.RED);
                } else if (dist >= far) {
                    recs[row][col].setFill(Color.GREEN);
                } else {
                    recs[row][col].setFill(Color.ORANGE);
                }
            }
        }

        recs[gRow][gCol].setFill(Color.BLUE);
    }

    @FXML
    public void showGhostExited(MouseEvent mouseEvent) {
        for (int row = 0; row < nRow; row++) {
            for (int col = 0; col < nCol; col++) {
                recs[row][col].setFill(cellColors[row][col]);
            }
        }
    }

    @FXML
    public void restartButtonClicked(MouseEvent mouseEvent) {
        init();
    }

}
