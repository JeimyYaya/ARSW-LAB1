
### Escuela Colombiana de Ingeniería
### Arquitecturas de Software - ARSW
## Ejercicio Introducción al paralelismo - Hilos - Caso BlackListSearch

- Jeimy Alejandra Yaya Martinez

### Dependencias:
####   Lecturas:
*  [Threads in Java](http://beginnersbook.com/2013/03/java-threads/)  (Hasta 'Ending Threads')
*  [Threads vs Processes]( http://cs-fundamentals.com/tech-interview/java/differences-between-thread-and-process-in-java.php)

### Descripción
  Este ejercicio contiene una introducción a la programación con hilos en Java, además de la aplicación a un caso concreto.
  
## Solución del laboratorio
**Parte I - Introducción a Hilos en Java**

1. De acuerdo con lo revisado en las lecturas, complete las clases CountThread, para que las mismas definan el ciclo de vida de un hilo que imprima por pantalla los números entre A y B.   
```
public class CountThread implements Runnable{
    private int A;
    private int B;

    public CountThread(int A, int B){
        this.A = A;
        this.B = B;

    }

    public void run(){
        for (int i = A; i <= B; i++){
            System.out.print(i + ", ");
        }
    }
}
```
2. Complete el método __main__ de la clase CountMainThreads para que:
	1. Cree 3 hilos de tipo CountThread, asignándole al primero el intervalo [0..99], al segundo [99..199], y al tercero [200..299].
	2. Inicie los tres hilos con 'start()'.
	```
	public class CountThreadsMain {
		
		public static void main(String a[]){
			Thread thread1 = new Thread(new CountThread(0,99));
			Thread thread2 = new Thread(new CountThread(99,199));
			Thread thread3 = new Thread(new CountThread(200, 299));

			thread1.start();
			thread2.start();
			thread3.start();
		}
		
	}
	```

	3. Ejecute y revise la salida por pantalla.      
	![alt text](img/image.png)

	4. Cambie el incio con 'start()' por 'run()'. Cómo cambia la salida?, por qué?.   
	![alt text](img/image-1.png)
	- Al utilizar __start()__ se ejecutan todos los hilos al mismo tiempo, por lo tanto los tres hilos se ejecutan de manera __concurrente__, por esta razón los números salen en desorden. Cuando se usa __run()__ solo se esta llamando a un metodo como cualquier otro, no se esta usando __multithreading__, entonces se ejecuta completamente el primero hilo, luego el segundo y finalmente el tercero.


**Parte II - Ejercicio Black List Search**


Para un software de vigilancia automática de seguridad informática se está desarrollando un componente encargado de validar las direcciones IP en varios miles de listas negras (de host maliciosos) conocidas, y reportar aquellas que existan en al menos cinco de dichas listas. 

Dicho componente está diseñado de acuerdo con el siguiente diagrama, donde:

- HostBlackListsDataSourceFacade es una clase que ofrece una 'fachada' para realizar consultas en cualquiera de las N listas negras registradas (método 'isInBlacklistServer'), y que permite también hacer un reporte a una base de datos local de cuando una dirección IP se considera peligrosa. Esta clase NO ES MODIFICABLE, pero se sabe que es 'Thread-Safe'.

- HostBlackListsValidator es una clase que ofrece el método 'checkHost', el cual, a través de la clase 'HostBlackListDataSourceFacade', valida en cada una de las listas negras un host determinado. En dicho método está considerada la política de que al encontrarse un HOST en al menos cinco listas negras, el mismo será registrado como 'no confiable', o como 'confiable' en caso contrario. Adicionalmente, retornará la lista de los números de las 'listas negras' en donde se encontró registrado el HOST.

![](img/Model.png)

Al usarse el módulo, la evidencia de que se hizo el registro como 'confiable' o 'no confiable' se dá por lo mensajes de LOGs:

INFO: HOST 205.24.34.55 Reported as trustworthy

INFO: HOST 205.24.34.55 Reported as NOT trustworthy


Al programa de prueba provisto (Main), le toma sólo algunos segundos análizar y reportar la dirección provista (200.24.34.55), ya que la misma está registrada más de cinco veces en los primeros servidores, por lo que no requiere recorrerlos todos. Sin embargo, hacer la búsqueda en casos donde NO hay reportes, o donde los mismos están dispersos en las miles de listas negras, toma bastante tiempo.

Éste, como cualquier método de búsqueda, puede verse como un problema [vergonzosamente paralelo](https://en.wikipedia.org/wiki/Embarrassingly_parallel), ya que no existen dependencias entre una partición del problema y otra.

Para 'refactorizar' este código, y hacer que explote la capacidad multi-núcleo de la CPU del equipo, realice lo siguiente:

1. Cree una clase de tipo Thread que represente el ciclo de vida de un hilo que haga la búsqueda de un segmento del conjunto de servidores disponibles. Agregue a dicha clase un método que permita 'preguntarle' a las instancias del mismo (los hilos) cuantas ocurrencias de servidores maliciosos ha encontrado o encontró.
```
public class HostBlackListTask implements Runnable{
    private final HostBlacklistsDataSourceFacade skds;
    private final String ipaddress;
    private final int startIndex;
    private final int endIndex;

    private int ocurrencesCount = 0;
    private int checkedListCount = 0;
    private final List<Integer> blacklistOcurrences = new LinkedList<>();

    public HostBlackListTask(HostBlacklistsDataSourceFacade skds, String ipaddress, int startIndex, int endIndex) {
        this.skds = skds;
        this.ipaddress = ipaddress;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }
    public void run() {
        for (int i = startIndex; i < endIndex; i++) {
            if (skds.isInBlackListServer(i, ipaddress)) {
                ocurrencesCount++;
                blacklistOcurrences.add(i);
            }
            checkedListCount++;
        }
    }

    public int getOcurrencesCount() {
        return ocurrencesCount;
    }

    public int getCheckedListCount() {
        return checkedListCount;
    }

    public List<Integer> getBlacklistOcurrences() {
        return blacklistOcurrences;
    }
}
```

2. Agregue al método 'checkHost' un parámetro entero N, correspondiente al número de hilos entre los que se va a realizar la búsqueda (recuerde tener en cuenta si N es par o impar!). Modifique el código de este método para que divida el espacio de búsqueda entre las N partes indicadas, y paralelice la búsqueda a través de N hilos. Haga que dicha función espere hasta que los N hilos terminen de resolver su respectivo sub-problema, agregue las ocurrencias encontradas por cada hilo a la lista que retorna el método, y entonces calcule (sumando el total de ocurrencuas encontradas por cada hilo) si el número de ocurrencias es mayor o igual a _BLACK_LIST_ALARM_COUNT_. Si se da este caso, al final se DEBE reportar el host como confiable o no confiable, y mostrar el listado con los números de las listas negras respectivas. Para lograr este comportamiento de 'espera' revise el método [join](https://docs.oracle.com/javase/tutorial/essential/concurrency/join.html) del API de concurrencia de Java. Tenga también en cuenta:

	* Dentro del método checkHost Se debe mantener el LOG que informa, antes de retornar el resultado, el número de listas negras revisadas VS. el número de listas negras total (línea 60). Se debe garantizar que dicha información sea verídica bajo el nuevo esquema de procesamiento en paralelo planteado.

	* Se sabe que el HOST 202.24.34.55 está reportado en listas negras de una forma más dispersa, y que el host 212.24.24.55 NO está en ninguna lista negra.

	```
	public class HostBlackListsValidator {

	    private static final int BLACK_LIST_ALARM_COUNT=5;
	    
	    /**
	     * Check the given host's IP address in all the available black lists,
	     * and report it as NOT Trustworthy when such IP was reported in at least
	     * BLACK_LIST_ALARM_COUNT lists, or as Trustworthy in any other case.
	     * The search is not exhaustive: When the number of occurrences is equal to
	     * BLACK_LIST_ALARM_COUNT, the search is finished, the host reported as
	     * NOT Trustworthy, and the list of the five blacklists returned.
	     * @param ipaddress suspicious host's IP address.
	     * @return  Blacklists numbers where the given host's IP address was found.
	     */
	    public List<Integer> checkHost(String ipaddress, int n){
	        
	        LinkedList<Integer> blackListOcurrences=new LinkedList<>();
	        HostBlacklistsDataSourceFacade skds=HostBlacklistsDataSourceFacade.getInstance();
	
	        int totalServers = skds.getRegisteredServersCount();
	        int patitionSize = totalServers / n;
	        int remainder = totalServers % n;
	
	        List<HostBlackListTask> tasks = new LinkedList<>();
	        List<Thread> threads = new LinkedList<>();
	                
	        int start = 0;
	        for (int i = 0; i < n; i++){
	            int end = start + patitionSize;
	            if (i == n-1){
	                end += remainder;
	            }
	                
	            HostBlackListTask task = new HostBlackListTask(skds, ipaddress, start, end);
	            Thread thread = new Thread(task);
	
	            tasks.add(task);
	            threads.add(thread);
	            thread.start();
	            start = end;
	        }
	
	        for(Thread t : threads){
	            try {
	                t.join();
	            }catch (InterruptedException e) {
	                Logger.getLogger(HostBlackListsValidator.class.getName()).log(Level.SEVERE, null, e);
	            }
	        }
	
	        int totalOcurrences = 0;
	        int totalCheckedLists = 0;
	
	        for(HostBlackListTask task : tasks){
	            totalOcurrences += task.getOcurrencesCount();
	            totalCheckedLists += task.getCheckedListCount();
	            blackListOcurrences.addAll(task.getBlacklistOcurrences());
	        }
	
	        if (totalOcurrences>=BLACK_LIST_ALARM_COUNT){
	            skds.reportAsNotTrustworthy(ipaddress);
	        }
	        else{
	            skds.reportAsTrustworthy(ipaddress);
	        }                
	        
	        LOG.log(Level.INFO, "Checked Black Lists:{0} of {1}", new Object[]{totalCheckedLists, skds.getRegisteredServersCount()});
	        
	        return blackListOcurrences;
	    }
	    
	    
    private static final Logger LOG = Logger.getLogger(HostBlackListsValidator.class.getName());
    
	}
	```


**Parte II.I Para discutir la próxima clase (NO para implementar aún)**

La estrategia de paralelismo antes implementada es ineficiente en ciertos casos, pues la búsqueda se sigue realizando aún cuando los N hilos (en su conjunto) ya hayan encontrado el número mínimo de ocurrencias requeridas para reportar al servidor como malicioso. Cómo se podría modificar la implementación para minimizar el número de consultas en estos casos?, qué elemento nuevo traería esto al problema?

#### Respuesta:
_Podría modificarse la implementación agregando una condición de parada global que se active cuando ya se cumpla el número mínimo de ocurrencias. Así los demás hilos se detienen y no realizan consultas innecesarias. Esto introduce la necesidad de sincronización entre hilos, porque ahora deben coordinarse para saber cuándo detenerse._


**Parte III - Evaluación de Desempeño**

A partir de lo anterior, implemente la siguiente secuencia de experimentos para realizar las validación de direcciones IP dispersas (por ejemplo 202.24.34.55), tomando los tiempos de ejecución de los mismos (asegúrese de hacerlos en la misma máquina):

1. Un solo hilo.   
![alt text](img/image-4.png)   
![alt text](img/image-5.png)
2. Tantos hilos como núcleos de procesamiento (haga que el programa determine esto haciendo uso del [API Runtime](https://docs.oracle.com/javase/7/docs/api/java/lang/Runtime.html)).   
![alt text](img/image-6.png)   
![alt text](img/image-8.png)
3. Tantos hilos como el doble de núcleos de procesamiento.   
![](img/image-9.png)   
![](img/image-7.png)
4. 50 hilos.   
![](img/image-10.png)   
![](img/image-11.png)
5. 100 hilos.      
![](img/image-12.png)

Al iniciar el programa ejecute el monitor jVisualVM, y a medida que corran las pruebas, revise y anote el consumo de CPU y de memoria en cada caso.     
![](img/image-13.png)

Con lo anterior, y con los tiempos de ejecución dados, haga una gráfica de tiempo de solución vs. número de hilos. Analice y plantee hipótesis con su compañero para las siguientes preguntas (puede tener en cuenta lo reportado por jVisualVM):     
![](img/image-14.png)

**Parte IV - Ejercicio Black List Search**

1. Según la [ley de Amdahls](https://www.pugetsystems.com/labs/articles/Estimating-CPU-Performance-using-Amdahls-Law-619/#WhatisAmdahlsLaw?):

	![](img/ahmdahls.png), donde _S(n)_ es el mejoramiento teórico del desempeño, _P_ la fracción paralelizable del algoritmo, y _n_ el número de hilos, a mayor _n_, mayor debería ser dicha mejora. Por qué el mejor desempeño no se logra con los 500 hilos?, cómo se compara este desempeño cuando se usan 200?. 

#### Respuesta:
_Según la Ley de Amdahl, el desempeño no crece de forma infinita con el número de hilos, porque siempre existe una fracción del programa que es serial (no puede paralelizarse). Al usar mas hilos, el [overhead](https://www.semanticscholar.org/topic/Overhead-(computing)/4163) de crear y gestionar hilos se vuelve mayor que el beneficio de dividir la parte paralela, generando incluso más tiempo perdido en coordinación que en cómputo._

_En comparación, con 200 hilos se obtiene un mejor equilibrio, el trabajo paralelo está suficientemente dividido para acelerar la ejecución, pero sin el exceso de overhead que aparece con 500. Por eso, 200 hilos alcanzan mejor desempeño práctico que 500._

2. Cómo se comporta la solución usando tantos hilos de procesamiento como núcleos comparado con el resultado de usar el doble de éste?.

#### Respuesta:
_Al comparar el número de hilos igual al de núcleos con el doble de hilos, se observó que el tiempo de ejecución disminuyó más de la mitad. Esto puede deberse a que con más hilos se logró esconder latencias (por ejemplo accesos a memoria o esperas de sincronización), permitiendo mayor uso efectivo de los núcleos._

_Sin embargo, en esta configuración el uso de CPU promedio bajó (ya que el tiempo total de ejecución fue menor), mientras que el uso de memoria aumentó (porque cada hilo extra consume stack y estructuras internas). En general, usar el doble de hilos puede mejorar el wall-time, pero trae más consumo de recursos y overhead._   
![alt text](img/image-15.png)

3. De acuerdo con lo anterior, si para este problema en lugar de 100 hilos en una sola CPU se pudiera usar 1 hilo en cada una de 100 máquinas hipotéticas, la ley de Amdahls se aplicaría mejor?. Si en lugar de esto se usaran c hilos en 100/c máquinas distribuidas (siendo c es el número de núcleos de dichas máquinas), se mejoraría?. Explique su respuesta.

#### Respuesta
_Si en lugar de correr 100 hilos en una sola CPU se ejecuta 1 hilo en cada una de 100 máquinas, el paralelismo real sería mucho mayor, ya que se aprovecha hardware independiente en lugar de compartir los mismos recursos. Esto haría que la fracción paralela P de Amdahl se aproveche mejor en la práctica._

_Sin embargo, Amdahl sigue aplicando: la parte serial del programa y la coordinación de resultados siguen siendo el límite. Además, aparecen nuevos costes como latencia de red, comunicación y sincronización entre máquinas, que reducen el speedup real._

_Si en lugar de eso se usan c hilos en 100/c máquinas (aprovechando los núcleos de cada una), el rendimiento sería mejor todavía: cada máquina usa su paralelismo interno de forma eficiente y la carga se distribuye entre varias. Esta opción es más equilibrada que concentrar todo en una sola CPU, aunque nunca se obtendrá un escalado perfectamente lineal por los costes de comunicación y la fracción secuencial del algoritmo._
	



