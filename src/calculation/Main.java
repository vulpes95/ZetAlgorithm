package calculation;

import form.Graph;
import parse.ReadTxtData;
import parse.ReadTmnData;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

import matrix.*;

public class Main {
    //путь файла исходных данных
    private final static String pathFile = "C:/Users/Don_Toton/Desktop/test.TMN";

    //объекс прочитанного файла, содержит исходную матрицу и карту
    public static ReadTmnData inputFile;

    //матрица значений прочитанного файла и ее карта пропусков
    public static MatrixTelemetry startMatrix;
    public static MatrixTelemetry mapStartMatrix;

    //лист столбцов с нулевыми дисперсиями
    public static ArrayList<Integer> listOfZeroDispersion = new ArrayList<>();

    //компетентная матрица для конкретного пропуска и ее карта
    public static MatrixCompetent matrixCompetent;
    public static MatrixCompetent matrixCompetentMap;

    //матрица, содержащая только восстановленные значения
    public static MatrixTelemetry reconstructedMatrix;
    //итоговая матрица (содержит исходные значения + дополнена восстановленными)
    public static MatrixTelemetry resultMatrix;
    //количество пропуском (используется при случайном генерировании матрицы пропусков)
    public static int quantityMiss = 300;

    public static void main(String[] args) {

        //создаем исходную матрицу значений и карту пропусков, указав путь файла tmn
        /*inputFile = new ReadTelemetryData(pathFile);
        startMatrix = inputFile.getInitalMatrix();
        mapStartMatrix=inputFile.getMapMatrix();*/

        //создаем исходную матрицу значений и карту пропусков из txt файла
        ReadTxtData inputFileTXT = new ReadTxtData("C:/Users/Don_Toton/Desktop/data_for_article.txt");
        startMatrix = inputFileTXT.getInitalMatrix();
        mapStartMatrix =inputFileTXT.getMapMatrix();

        //вывод исходных матриц
        /*startMatrix.show();
        mapStartMatrix.show();*/

        /* Функция, которая случайным образом генерируем карту пропусков (с количеством пропусков quantityMiss).
           Использовал 1 раз, затем загружал данную конфигурацию пропусков для создания одинаковых начальных
           условий для разных конфигураций восстановления (разных размеров компетентных матриц)
        ruinInputMatrix(quantityMiss, inputFileTXT.numberOfRows, inputFileTXT.numberOfCollums);*/

        // загрузка конфигурации пропусков, созданной при помощи метода ruinInputMatrix()
        loadMatrixMap("C:/Users/Don_Toton/Desktop/map_for_article.txt");
        mapStartMatrix.show();


        /*нормализация по дисперсии матрицы значений
          убрал нормализацию, т.к без нее результаты получались намного лучше
        startMatrix.normalizeByDispersion(mapStartMatrix);
        System.out.println("Нормализованная по дисперсии");
        startMatrix.show();*/

        //получаем колонки с нулевыми дисперсиями
        for (int j = 0; j <startMatrix.N ; j++)
            if (startMatrix.getDespersionCollum(mapStartMatrix,j)==0) listOfZeroDispersion.add(j);
        System.out.println("cтолбцы с нулевой дисперсией: ");
        for(Integer i:listOfZeroDispersion) System.out.print(i+"  ");
        System.out.println();

        //создаем результирующую матрицу и ее карту (матрица, которая будет содержать только восстановленные значени)
        reconstructedMatrix = new MatrixTelemetry(startMatrix.M,startMatrix.N);
        //эта матрица будет содержать исходные + восстановленные значения
        resultMatrix = new MatrixTelemetry(startMatrix.M,startMatrix.N);
        MatrixTelemetry resultMap = new MatrixTelemetry(startMatrix.M,startMatrix.N);

        //Перебираем все элементы с пропусками
        for (int i = 0; i < mapStartMatrix.M ; i++) {
            for (int j = 0; j < mapStartMatrix.N ; j++) {

                //если в карте этот элемент отмечен, как пропущенный, то пытаемся восстановить
                if (mapStartMatrix.get(i,j)==1) {

                    //создание компетентной матрицы для конкретного пропуска и его предсказание (в случае успешного создания)
                    //System.out.println("получаем компетентную матрицу для элемента ["+i+"] ["+j+"]");
                    if (getCompetentMatrix(startMatrix, mapStartMatrix, i, j)) {

                        reconstructedMatrix.set(i, j, matrixCompetent.resultPrediction(matrixCompetentMap));
                        resultMap.set(i,j,1);
                    }
                    //System.out.println();
                }
            }
        }

       /* Т.к убрана дисперсия, обратно на нее домножать не нужно
        System.out.println("Умножаем на дисперсию");
        startMatrix.deNormalizeByDispersion(mapStartMatrix);
        startMatrix.show()*/;

        /*
        System.out.println("Восстановленные значения");
        reconstructedMatrix.show();
        System.out.println("Карта восстановленных значений ");
        resultMap.show();*/

        // вычисление суммарной ошибки восстановления
        double error =calculateResultError(startMatrix,reconstructedMatrix,resultMap);

        //вставка восстановленных значений в исходную матрицу
        insert(reconstructedMatrix, resultMap, startMatrix);
        System.out.println("Начальная матрица:");
        startMatrix.show();
        System.out.println("Конечная матрица");
        resultMatrix.show();
        System.out.println("[финальная погрешность]"+error);

        Graph graph = new Graph(startMatrix,mapStartMatrix, resultMatrix);
        graph.setVisible(true);
    }

    public static double calculateResultError( MatrixTelemetry inital, MatrixTelemetry matrix, MatrixTelemetry map){
        double error=0,temp;
        for (int i = 0; i <inital.M ; i++) {
            for (int j = 0; j < inital.N; j++) {
                if (map.get(i,j)==1) {
                    if (inital.get(i,j)!=0) {
                        temp = Math.abs((inital.get(i, j) - matrix.get(i, j)) / inital.get(i, j));
                        error+=temp;
                        System.out.println("i= "+i+" j= "+j+" error= "+temp);
                    } else {
                        temp =  Math.abs(inital.get(i, j) - matrix.get(i, j));
                        error+=temp;
                        System.out.println("[0] error= "+temp);
                    }
                }
            }
        }

        return error/quantityMiss;
    }

    public static void loadMatrixMap(String path){
        StringBuilder dataFromFile = new StringBuilder();
        String s,inputSimpleData=null;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(path)))){
            while ((s=reader.readLine())!=null){
                dataFromFile.append(s).append("\n");
            }
          inputSimpleData=dataFromFile.toString();

        }catch (IOException e){
            System.out.println("Файл не найден");
        }
        String[] arrayOfRow= inputSimpleData.split("\n");
        for (int i = 0; i <arrayOfRow.length ; i++) {

            String newRow = arrayOfRow[i].replace(",",".");
            String dataInOneRow []= newRow.split("\\s+");
            for (int j = 0; j <dataInOneRow.length ; j++) {
                 mapStartMatrix.set(i,j,Double.parseDouble(dataInOneRow[j]));
            }
        }
    }


    public static void ruinInputMatrix(int quantitySpoilt, int quantityOfRows, int quantityOfCollums){
        int count = 0;
        int i,j;
        while (count<quantitySpoilt){
//            i = (int) ((Math.random()*1000) % mapStartMatrix.M);
//            j = (int) ((Math.random()*1000) % mapStartMatrix.N);
            i = (int) (Math.random()* (quantityOfRows));
            j = (int) (Math.random()*(quantityOfCollums));

            if (j==55) continue;

            if (mapStartMatrix.get(i,j)!=1) {
                mapStartMatrix.set(i, j, 1);
                count++;
              //  System.out.println("i= "+i+" j="+j);
            }
        }
    }
    
    public static void insert( MatrixTelemetry reconstructedMatrix,MatrixTelemetry resultMap,MatrixTelemetry startMatrix ){
        for (int i = 0; i <startMatrix.M ; i++) {
            for (int j = 0; j < startMatrix.N; j++) {
                if (resultMap.get(i,j)==1) resultMatrix.set(i,j,reconstructedMatrix.get(i,j));
                else resultMatrix.set(i,j,startMatrix.get(i,j));
            }
        }
    }

    // возвращает компетентную матрицу для указанного элемента
    public static boolean getCompetentMatrix(MatrixTelemetry matrix, MatrixTelemetry map, int iSkip, int jSkip){

        //если дисперсия столбца восстанавливаемого пропуска=0, то не обрабатываем его
        if (listOfZeroDispersion.indexOf(jSkip)!=-1) {
            double avarage = startMatrix.getAverageCollum(map,jSkip);
            reconstructedMatrix.set(iSkip,jSkip,avarage);
           //System.out.println("Дисперсия столбца восстанавлимаемого пропуска =0 ");
            return false;
        }

        //задаем диапазон поиска компетентный строк
        int searchRange=4; //количество строк сверху и снизу для поиска компетентных строк; если 4, то всего 8
        int competentRows=6, competentCollum=6;

        //предотвращаем выход за диапазон матрицы
        int topBorder=0, bottomBorder=0, temp;
        topBorder=iSkip-searchRange;
        bottomBorder=iSkip+searchRange+1;

        //предотвращаем выход за диапазон матрицы
        if (topBorder<0){
            temp=Math.abs(topBorder);
            topBorder=0;
            bottomBorder+=temp;

        } else if (bottomBorder>matrix.M){
            temp=bottomBorder-matrix.M;
            bottomBorder=matrix.M;
            topBorder-=temp;
        }

        //считаем компетентности строк searchRange строк снизу и сверху указанной в iSkip
        Map<Integer,Double> listOfCompetentRows = new LinkedHashMap<>();
        double competent=0;
        for (int i = topBorder; i <bottomBorder ; i++) {
            // Компетентная строка не может иметь пропуска в j
            if ((i==iSkip) ||(map.get(i,jSkip)==1)) continue;

            competent = matrix.getCompetentOfRow(map,iSkip,i);
            listOfCompetentRows.put(i,competent);
        }

        // Сортируем список по убыванию величины компетентности строк
        Integer [] keys = getSortKeys(listOfCompetentRows);

        // Отбираем в матрицу указанное competentRows число строк с наибольшей компетентностью
        ArrayList<Integer> selectedRow = new ArrayList<>();
        selectedRow.add(iSkip);
        for (int i = 0; i <competentRows ; i++)
            selectedRow.add(keys[i]);
        Collections.sort(selectedRow);

        // Теперь у нас поменялся индекс строки, в которой находится пропуск
        int newISkip=selectedRow.indexOf(iSkip);

        //Создаем карту компетентностей строк для компетентной матрицы (нужно будет при подборе
        // степени влияния компетентности)
        Map<Integer,Double> competentOfRowsOfCompetentMatrix = new LinkedHashMap<>();
        for (int i = 0; i <competentRows+ 1; i++) {
            if (i==newISkip) continue;
            competentOfRowsOfCompetentMatrix.put(i, listOfCompetentRows.get(selectedRow.get(i)));
        }

        //создаем матрицу и ее карту с выбранными компетентными строками
        MatrixCompetent matrixCompetentRows = new MatrixCompetent(competentRows+1,startMatrix.N);
        MatrixCompetent matrixCompetentRowsMap  = new MatrixCompetent(competentRows+1,startMatrix.N);

        //внесение в объекты матриц индексов строк для удобного вывода
        matrixCompetentRows.numeration=selectedRow;
        matrixCompetentRowsMap.numeration=selectedRow;

        //копирование указанного списка индексов строк в новую матрицу
        matrixCompetentRows.copyRow(matrix,selectedRow);
        matrixCompetentRowsMap.copyRow(map,selectedRow);

        //вывод матриц на экран
        //matrixCompetentRows.show();
        // matrixCompetentRowsMap.show();

        /*****************************************************************************************************************/
        /************************************         Для столбцов  ******************************************************/
        /*****************************************************************************************************************/

        //получаем лист нежелательных для предсказания колонок
        ArrayList<Integer> listOfDepricateCollum=getIndexOfDepricateCollum(matrixCompetentRowsMap,competentRows );

        //добавляем к списку нежелательных строк строки с нулевой дисперсий
        for (int j = 0; j <matrixCompetentRows.N ; j++) {

            if (listOfDepricateCollum.contains(j)) continue;

            if (matrixCompetentRows.getDespersionCollum(matrixCompetentRowsMap, j) == 0){
                listOfDepricateCollum.add(j);
            }
        }
//        System.out.println("Нежелательные столбцы компетентной матрицы ");
//        for (Integer i: listOfDepricateCollum) System.out.print(i+" ");
//        System.out.println();


        // получаем карту <Индекс, Значение> компетентностей столбцов                                         !CHEKED+!
        Map<Integer,Double> listOfCompetentCollum = new LinkedHashMap<>();
        for (int j = 0; j <matrixCompetentRows.N ; j++) {
            // Компетентный столбец не может иметь пропуска в i строке
            if ((j==jSkip) ||(matrixCompetentRowsMap.get(newISkip,j)==1)) {  // iSKIP стало другим
                continue;
            }
            // пропускаем нежелательные столбцы
            if (listOfDepricateCollum.contains(j)) {
                continue;
            }
            // получаем карту компетентнностей столбцов в формате <Индекс, Значение>
            competent = matrixCompetentRows.getCompetentOfCollum(matrixCompetentRowsMap,j,jSkip);
            listOfCompetentCollum.put(j,competent);
        }

        // Сортируем список по убыванию величины компетентности столбцов
        Integer [] keysCollum = getSortKeys(listOfCompetentCollum);

        // Отбираем в матрицу указанное competentCollum число столбцов с наибольшей компетентностью
        ArrayList<Integer> selectedCollum = new ArrayList<>();
        selectedCollum.add(jSkip);
        for (int i = 0; i <competentCollum ; i++)
            selectedCollum.add(keysCollum[i]);
        Collections.sort(selectedCollum);

        // Теперь у нас поменялся индекс столбца, в которой находится пропуск
        int newJSkip=selectedCollum.indexOf(jSkip);

        //создаем карту компетентностей столбцов для компетентной матрицы (нужно будет пи подборе
        // степени влияния компетентности)
        Map<Integer,Double> competentOfCollumOfCompetentMatrix = new LinkedHashMap<>();
        for (int i = 0; i <competentRows+ 1; i++) {
            if (i==newJSkip) continue;
            competentOfCollumOfCompetentMatrix.put(i,  listOfCompetentCollum.get(selectedCollum .get(i)));
        }

        //создаем компетентную матрицу и ее карту с выбранными компетентными столбцами
        MatrixCompetent matrixCompetentCollum = new MatrixCompetent(competentRows+1,competentCollum+1);
        MatrixCompetent matrixCompetentCollumMap  = new MatrixCompetent(competentRows+1,competentCollum+1);

        //внесение в объекты матриц индексов строк для удобного вывода
        matrixCompetentCollum.numeration=selectedRow;
        matrixCompetentCollumMap.numeration=selectedRow;

        //внесение в объекты матриц названий столбцов для удобного вывода
        //matrixCompetentCollum.setCaptions(selectedCollum);
        //matrixCompetentCollumMap.setCaptions(selectedCollum);

        //копирование указанного списка индексов столбцов в новую матрицу
        matrixCompetentCollum.copyCollum(matrixCompetentRows,selectedCollum);
        matrixCompetentCollumMap.copyCollum(matrixCompetentRowsMap,selectedCollum);

        //устанавливаем компетентности компетентной матрицы
        matrixCompetentCollum.setCompetents(competentOfCollumOfCompetentMatrix,competentOfRowsOfCompetentMatrix);

        //вывод матриц на экран
         //matrixCompetentCollum.show();
         // matrixCompetentCollumMap.show();

        ////внесение в объекты матриц измененных коордиат пропуска
        matrixCompetentCollum.setISkipJSkip(newISkip, newJSkip);
        matrixCompetentCollumMap.setISkipJSkip(newISkip, newJSkip);

        //установка полей для дальнейшего доступа к созданной компетентной матрице
        matrixCompetent=matrixCompetentCollum;
        matrixCompetentMap=matrixCompetentCollumMap;

        return true;
    }

    //Перебераем столбцы, ищем те, в которых много пропусков
    public static ArrayList<Integer> getIndexOfDepricateCollum(MatrixCompetent matrixCompetentRowsMap, int competentRows){

        ArrayList<Integer> list = new ArrayList<>();
        int count;

        for (int j = 0; j <matrixCompetentRowsMap.N ; j++) {
            count=0;
            for (int i = 0; i< matrixCompetentRowsMap.M; i++) {

                if (matrixCompetentRowsMap.get(i,j)==1) count++;
            }

            if (count>(competentRows/2-1)) {
                list.add(j);
            }
        }
        return list;
    }

    //возвращает индексы строк (столбцов) с наибольшей компетентностью
    public static Integer[] getSortKeys(Map<Integer,Double> map){

        Double [] valueCollum = map.values().toArray(new Double[map.size()]);
        Integer [] keysCollum = map.keySet().toArray(new Integer [map.size()] );
        double c1=0; int c2=0;
        for (int i = map.size()-1; i >0 ; i--) {
            for (int j = 0; j <i ; j++) {
                if (valueCollum[j]<valueCollum[j+1]){
                    c1=valueCollum[j];
                    valueCollum[j]=valueCollum[j+1];
                    valueCollum[j+1]=c1;

                    c2=keysCollum[j];
                    keysCollum[j]=keysCollum[j+1];
                    keysCollum[j+1]=c2;
                }
            }
        }
        return keysCollum;
    }
}











