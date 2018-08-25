package edu.unh.cs.ai.realtimesearch.util

import java.util.*

/**
 * Created by Daniel on 08/01/2016.
 */
class GH_heap<E : SearchQueueElement<*>>(private val w: Double, val key: Int, fmin: Double, private var dmin: Double,
                                         private val useFR: Boolean, private val useD: Boolean,
                                         private val isFocalized: Boolean, private val useWApriority: Boolean) {
    private val IsFocalizedPrecision = Math.pow(10.0, -13.0)
    private val debug = false
    private val countF = HashMap<Double, Double>()
    private val countD = HashMap<Double, Double>()
    //    private BinHeap<E> heap;
    private var tree: TreeMap<gh_node, ArrayList<E>>? = null
    private var outOfFocalTree: TreeMap<gh_node, ArrayList<E>>? = null
    private var treeF: TreeMap<gh_node, ArrayList<E>>? = null
    //        testHat();
    var fmin: Double = 0.toDouble()
        private set
    private var isOptimal: Boolean = false
    private var bestNode: gh_node? = null
    private var bestList: ArrayList<E>? = null
    private val comparator: Comparator<gh_node>
    private var GH_heapSize: Int = 0

    val fminCount: Double
        get() {
            val counter = countF[fmin]
            if (counter == 0.0) {
                println("[INFO GH_heap] getFminCount == 0")
            }
            return counter!!
        }

    val isEmpty: Boolean
        get() = if (isFocalized)
            tree!!.isEmpty() && outOfFocalTree!!.isEmpty()
        else
            tree!!.isEmpty()

    init {
        this.comparator = ghNodeComparator()
        this.tree = TreeMap(this.comparator)
        this.fmin = fmin

        if (useFR) this.treeF = TreeMap(Fcomparator())
        if (isFocalized) this.outOfFocalTree = TreeMap(this.comparator)
    }

    fun setOptimal(fmin: Double) {
        this.isOptimal = true
    }

    fun add(e: E) {
        //    testHat();
        if (debug) debugNode(e, "add")
        //        if(result.generated % 10000 == 0) System.out.print("\rgenerated(add):"+result.generated);

        count_add(e)
        val node = gh_node(e)
        val treeOfNode = if (node.inTree) tree else outOfFocalTree
        /*        if(!node.inTree){
            System.out.println("[INFO] add Node not in Focal!");
        }*/

        val list: ArrayList<E>

        if (treeOfNode!!.containsKey(node)) {
            list = treeOfNode[node]!!
        } else {
            list = ArrayList()
        }
        e.setIndex(this.key, list.size)
        list.add(e)
        treeOfNode[node] = list

        if (useFR) {
            val listF: ArrayList<E>
            if (treeF!!.containsKey(node)) {
                listF = treeF!![node]!!
            } else {
                listF = ArrayList()
            }
            listF.add(e)
            treeF!![node] = listF
        }

        if (node.inTree) {
            if (this.bestNode != null) {
                if (this.comparator.compare(bestNode, node) > 0) {
                    if (debug) debugNode(e, "add2")
                    bestNode = node
                    bestList = list
                }
            } else {
                if (debug) debugNode(e, "add3")
                bestNode = node
                bestList = list
            }
        }
        //    testHat();
    }

    private fun count_add(e: E) {
        val f = e.f
        GH_heapSize++
        if (countF.containsKey(f))
            countF[f] = countF[f]!! + 1
        else {
            countF[f] = 1.0
            /*            if(!isOptimal) {//fmin might change/decrease
//                if(fmin == 0.0) fmin = f;
*//*                if (tree.size()+outOfFocalTree.size() == 0) {//tree is empty
                    fmin = f;
                }
                if (fmin > f+ IsFocalizedPrecision) {//might occur due to rounding of f;
                    throw new IllegalArgumentException("[ERROR] fmin:"+fmin+" > f:"+f+", IsFocalizedPrecision:"+ IsFocalizedPrecision +" heuristic might be inconsistent OR adding doubles round error");
                }*//*
            }*/
        }
        if (useD) {
            val fd = e.d + e.depth
            if (countD.containsKey(fd))
                countD[fd] = countD[fd]!! + 1
            else {
                countD[fd] = 1.0
                //                if(dmin == 0.0) dmin = fd;
                /*                if (!isOptimal) {//fmin might change/decrease
                    if (tree.size() + outOfFocalTree.size() == 0) {//tree is empty
                        dmin = fd;
                    }
                    if (dmin - fd > 0) {//might occur due to rounding of d??? - this should never happen;
                        throw new IllegalArgumentException("[ERROR] dmin>d");
                    }
                }*/
            }
        }
    }

    fun poll(): E {
        val e = bestList!![0]
        return remove(e)
    }

    fun peek(): E? {
        return bestList?.get(0)
    }

    fun peekF(): E {
        if (!useFR) throw IllegalArgumentException("[ERROR] peekF can only be used when useFR = true")
        val bestF = treeF!!.firstKey()
        val listF = treeF!![bestF]!!
        return listF[0]
    }

    fun update(e: E) {
        throw UnsupportedOperationException("Invalid operation for GH_heap, use remove and add instead")
    }

    fun updateF(oldNode: E, newNode: E) {
        //        test();
        /*        gh_node oldPos = new gh_node(oldG,oldH);
        ArrayList<E> oldList = tree.get(oldPos);
        E toRemove = null;
        for (E e:oldList) {
            if(NodePackedComparator.compare(updatedNode,e)==0){
                toRemove = e;
            }
        }
        if(toRemove != null){
            remove(toRemove);
        }
        else{
            System.out.println("can not remove");
        }
        add(updatedNode);*/
        //        test();

    }


    private fun reorder() {
        //        int buckets = tree.size();//for paper debug
        //        int nodes = 0;//for paper debug
        val tempTree = TreeMap<gh_node, ArrayList<E>>(comparator)

        run {
            val it = tree!!.entries.iterator()
            while (it.hasNext()) {
                val entry = it.next()
                val node = entry.key
                val list = entry.value
                //            nodes +=list.size();
                it.remove()
                node.calcPotential()
                tempTree[node] = list
            }
        }

        if (isFocalized) {
            val tempOutOfFocalTreeTree = TreeMap<gh_node, ArrayList<E>>(comparator)
            val it = outOfFocalTree!!.entries.iterator()
            while (it.hasNext()) {
                val entry = it.next()
                val node = entry.key
                val list = entry.value
                it.remove()
                if (debug) debugNode(node, "reorder")
                node.calcPotential()
                if (debug) debugNode(node, "reorder1")
                if (node.inTree)
                    tempTree[node] = list
                else
                    tempOutOfFocalTreeTree[node] = list
            }
            outOfFocalTree = tempOutOfFocalTreeTree
        }
        tree = tempTree
        if (bestNode == null) {
            bestNode = tree!!.firstKey()
            val best = bestNode
            bestList = tree!![best]
        }
    }

    fun size(): Int {
        return GH_heapSize
    }

    fun clear() {
        countF.clear()
        countD.clear()
        tree!!.clear()
        outOfFocalTree!!.clear()
        bestNode = null
        bestList!!.clear()
    }

    fun remove(e: E): E {
        //        testHat();
        if (debug) debugNode(e, "remove")
        val node = gh_node(e)
        val treeOfNode = if (node.inTree) tree else outOfFocalTree
        val list = treeOfNode!![node]

        /*        if(!node.inTree){
            System.out.println("[INFO] remove Node not in Focal!");
        }*/

        if (list == null) {
            val treeOfNode2 = if (!node.inTree) tree else outOfFocalTree
            val list2 = treeOfNode2!![node]
            if (list2 == null) {
                println("\u001B[31m" + "[WARNING] This Node is in NO list" + "\u001B[0m")
            } else {
                println("\u001B[31m" + "[WARNING] This Node is in the WRONG list" + "\u001B[0m")
            }
            println("\ne:$e")
            println("tree size:" + treeOfNode.size)
        }
        if (list!!.isEmpty())
            println("\u001B[31m" + "[WARNING] list is empty, can not remove" + "\u001B[0m")
        list.remove(e)
        e.setIndex(this.key, -1)

        if (list.isEmpty()) {
            treeOfNode.remove(node)
            if (this.comparator.compare(node, bestNode) <= 0 && node.inTree) {
                if (tree!!.isEmpty()) {
                    if (debug) debugNode(e, "remove2")
                    bestNode = null
                    bestList = null
                    //                    System.out.println("[WARNING] Tree is empty");
                } else {
                    if (debug) debugNode(e, "remove3")
                    bestNode = tree!!.firstKey()
                    val best = bestNode
                    bestList = tree!![best]
                    if (debug) debugNode(e, "remove4")
                }
            }
        } else {
            treeOfNode[node] = list
        }

        if (useFR) {
            val listF = treeF!![node]
            if (listF != null) {
                if (listF.isEmpty())
                    println("\u001B[31m" + "[WARNING] listF is empty, can not remove" + "\u001B[0m")
            }
            listF?.remove(e)
            if (listF != null) {
                if (listF.isEmpty())
                    treeF!!.remove(node)
            }
        }

        count_remove(e)
        if (bestNode == null) {
            println("[WARNING] bestNode == null")
            reorder()
        }
        //        testHat();
        return e
    }

    private fun count_remove(e: E) {
        val f = e.f
        val fd = e.d + e.depth
        GH_heapSize--
        if (!countF.containsKey(f)) {
            countF[f] = 0.0
        }
        countF[f] = countF[f]!! - 1
        if (countF[f]!! == 0.0) {
            countF.remove(f)
            if (!isOptimal) {
                //fmin might increase, if the heuristic is consistent fmin should not decrease
                if (f <= fmin && (tree!!.size > 0 || outOfFocalTree!!.size > 0)) {//find next lowest
                    val prevFmin = fmin
                    fmin = Integer.MAX_VALUE.toDouble()
                    val it = countF.entries.iterator()
                    while (it.hasNext()) {
                        val pair = it.next() as Map.Entry<*, *>
                        val key = pair.key as Double
                        if (fmin >= key) {
                            fmin = key
                        }
                    }
                    // for cases where the heuristic is admissible but not consistent
                    if (prevFmin >= fmin) {
                        //                        System.out.print("\r[INFO GH_heap] heuristic is not consistent: prevFmin = "+prevFmin+" > fmin = "+fmin+" , inconsistency:"+(prevFmin-fmin));
                        fmin = prevFmin
                    } else {
                        reorder()
                        //                        System.out.print("\r[INFO GH_heap] new fmin = "+fmin);
                    }
                } else {
                    if (debug) debugNode(e, "count_remove")
                }
            }
        }
        if (useD) {
            if (!countD.containsKey(fd)) {
                countD[fd] = 0.0
            }
            countD[fd] = countD[fd]!! - 1
            if (countD[fd] == 0.0) {
                countD.remove(fd)
                if (!isOptimal) {
                    //dmin might increase, if the heuristic is consistent dmin should not decrease
                    if (fd <= dmin && (tree!!.size > 0 || outOfFocalTree!!.size > 0)) {//find next lowest
                        val prevDmin = dmin
                        dmin = Integer.MAX_VALUE.toDouble()
                        val it = countD.entries.iterator()
                        while (it.hasNext()) {
                            val pair = it.next() as Map.Entry<*, *>
                            val key = pair.key as Double
                            if (dmin >= key) {
                                dmin = key
                            }
                        }
                        // for cases where the heuristic is admissible but not consistent
                        if (prevDmin > dmin) {
                            //                            System.out.print("\r[INFO GH_heap] heuristic is not consistent: prevDmin = "+prevDmin+" > dmin = "+dmin+" , inconsistency:"+(prevDmin-dmin));
                            dmin = prevDmin
                        } else {
                            reorder()
                            //                            System.out.print("\r[INFO GH_heap] new dmin = "+dmin);
                        }
                    }
                }
            }
        }
    }

    fun testHat() {
        /*        for(Iterator<Map.Entry<gh_node,ArrayList<E>>> it = tree.entrySet().iterator(); it.hasNext();){
            Map.Entry<gh_node,ArrayList<E>> entry = it.next();
            ArrayList<E> list = entry.getValue();
            for(int i=list.size()-1 ; i>=0 ; i--){
                double Val = list.get(i).getF();
                if(Val < fmin){
                    System.out.println("test Failed! Val < fmin");
                }
                countF.put(Val,countF.get(Val)-1);
                if(countF.get(Val)<0){
                    System.out.println("test failed! countF.get("+Val+")<0");
                }
            }
        }

        for(Iterator<Map.Entry<gh_node,ArrayList<E>>> it = tree.entrySet().iterator(); it.hasNext();){
            Map.Entry<gh_node,ArrayList<E>> entry = it.next();
            ArrayList<E> list = entry.getValue();
            for(int i=list.size()-1 ; i>=0 ; i--){
                double Val = list.get(i).getF();
                if(Val < fmin){
                    System.out.println("test Failed! Val < fmin");
                }
                countF.put(Val,countF.get(Val)+list.size());
            }
        }*/
    }

    private fun debugNode(e: E, from: String) {
        val node = gh_node(e)
        debugNode(node, from)
    }

    private fun debugNode(node: gh_node, from: String) {
        if (from === "count_remove") {
            if (fmin == 455.27838751346) {
                //check that fmin is still OK
                var found_fmin = Integer.MAX_VALUE.toDouble()
                val it = countF.entries.iterator()
                while (it.hasNext()) {
                    val pair = it.next() as Map.Entry<*, *>
                    val key = pair.key as Double
                    if (found_fmin >= key) {
                        found_fmin = key
                    }
                }
                // there is a problem with fmin
                if (found_fmin > fmin) {
                    print("\r[ERROR GH_heap] check the fmin counter. fmin=$fmin, found_fmin=$found_fmin")
                }
            }
        }

        /*        if(node.f + IsFocalizedPrecision < fmin){
            System.out.println("++++++++++++++++++++++");
            System.out.println("[INFO] fmin decreased:"+from);
            System.out.println(fmin);
            System.out.println(node.f);
            System.out.println(result.generated);
            System.out.println("++++++++++++++++++++++");
        }*/
        /*        if((node.d == 8.0 && node.g == 6.0)
                || (node.d == 8.0 && node.h == 6.0))
        {
            System.out.println("++++++++++++++++++++++");
            System.out.println("[INFO] "+from+":");
            System.out.println(node);
            System.out.println(fmin);
            System.out.println(result.generated);
            System.out.println("++++++++++++++++++++++");
        }*/

        /*        if(bestNode != null && bestNode.inTree == false && bestList != null){
            System.out.println("++++++++++++++++++++++");
            System.out.println("[INFO] Best node not in Tree, from:"+from);
            System.out.println(bestNode);
            System.out.println(fmin);
            System.out.println(bestNode.f-w*fmin);
            System.out.println(result.generated);
            System.out.println("++++++++++++++++++++++");
        }*/

        val whatWentWrong = ""
        //        if(bestList == null) whatWentWrong+="bestList == null,";
        //        if(bestNode == null) whatWentWrong+="bestNode == null,";
        //        if(tree.isEmpty()) whatWentWrong+="tree.isEmpty(),";
        //        if(outOfFocalTree.isEmpty()) whatWentWrong+="outOfFocalTree.isEmpty(),";
        if (whatWentWrong !== "") {
            println("++++++++++++++++++++++")
            println("SOMETHING WENT WRONG WITH THIS NODE")
            println("What went wrong?:$whatWentWrong")
            println("[INFO] $from:")
            println(node)
//            System.out.println(result.generated)
            println("++++++++++++++++++++++")
        }
    }

    private fun test() {
        run {
            val it = tree!!.entries.iterator()
            while (it.hasNext()) {
                val entry = it.next()
                val list = entry.value
                val Val = list[0].f
                if (Val < fmin) {
                    println("test Failed! Val < fmin:tree")
                }
                countF[Val] = countF[Val]!! - list.size
                if (countF[Val]!! < 0) {
                    println("test failed! countF.get($Val)<0")
                }
            }
        }

        if (isFocalized) {
            val it = outOfFocalTree!!.entries.iterator()
            while (it.hasNext()) {
                val entry = it.next()
                val list = entry.value
                val Val = list[0].f
                if (Val < fmin) {
                    println("test Failed! Val < fmin:outOfFocalTree")
                }
                countF[Val] = countF[Val]!! - list.size
                if (countF[Val]!! < 0) {
                    println("test failed! countF.get($Val)<0 : outOfFocalTree")
                }
            }
        }

        val it = tree!!.entries.iterator()
        while (it.hasNext()) {
            val entry = it.next()
            val list = entry.value
            val Val = list[0].f
            if (Val < fmin) {
                println("test Failed! Val < fmin")
            }
            countF[Val] = countF[Val]!! + list.size
        }
    }


    inner class gh_node(e: E) {
        internal var g: Double = 0.toDouble()
        internal var h: Double = 0.toDouble()
        internal var potential: Double = 0.toDouble()

        internal var depth: Double = 0.toDouble()
        internal var hHat: Double = 0.toDouble()
        internal var d: Double = 0.toDouble()
        internal var dHat: Double = 0.toDouble()

        internal var f: Double = 0.toDouble()

        internal var inTree = true
        internal var cost: Double = 0.toDouble()
        internal var estimator: Double = 0.toDouble()

        override fun toString(): String {
            return "gh_node{" +
                    "g=" + g +
                    ", h=" + h +
                    ", depth=" + depth +
                    ", d=" + d +
                    ", f=" + f +
                    ", inTree=" + inTree +
                    ", potential=" + potential +
                    ", hHat=" + hHat +
                    ", dHat=" + dHat +
                    '}'.toString()
        }

        init {
            this.f = e.f

            this.g = e.g
            this.h = e.h
            this.depth = e.depth
            this.hHat = e.hHat
            this.d = e.d
            this.dHat = e.dHat

            calcPotential()
        }

        fun calcPotential() {

            if (isFocalized) inTree = this.f + IsFocalizedPrecision <= w * fmin

            if (this.h == 0.0 || this.d == 0.0) {
                this.potential = java.lang.Double.MAX_VALUE
            } else {
                if (useD) {
                    if (useWApriority)
                        this.potential = this.depth + this.d * w
                    else
                        this.potential = (w * dmin - this.depth) / this.d
                    this.cost = this.depth
                    this.estimator = this.d
                } else {
                    if (useWApriority)
                        this.potential = this.g + this.h * w
                    else
                        this.potential = (w * fmin - this.g) / this.h
                    this.cost = this.g
                    this.estimator = this.h
                }
            }
        }

    }

    /**
     * The nodes comparator class
     */
    protected inner class ghNodeComparator : Comparator<gh_node> {

        override fun compare(a: gh_node, b: gh_node): Int {
            // First compare by potential (bigger is preferred), then by f (smaller is preferred), then by g (smaller is preferred)
            if (a.potential > b.potential) return -1
            if (a.potential < b.potential) return 1

            if (a.cost < b.cost) return -1
            if (a.cost > b.cost) return 1

            if (a.estimator < b.estimator) return -1
            if (a.estimator > b.estimator) return 1

            // From here on it is a tiebreak for cases where we have focal and non-focal nodes
            if (a.f < b.f) return -1
            if (a.f > b.f) return 1

            if (a.h < b.h) return -1
            if (a.h > b.h) return 1

            if (a.g < b.g) return -1
            if (a.g > b.g) return 1

            return if (a.inTree != b.inTree) 1 else 0

        }
    }

    /**
     * The nodes comparator class based of F value
     */
    protected inner class Fcomparator : Comparator<gh_node> {

        override fun compare(a: gh_node, b: gh_node): Int {
            if (a.f < b.f) return -1
            if (a.f > b.f) return 1

            if (a.h < b.h) return -1
            return if (a.h > b.h) 1 else 0

        }
    }

}
