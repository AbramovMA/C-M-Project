package ndfs.mcndfs_1_naive;

public class Triple<T1, T2, T3>{         
    public T1 first;
    public T2 second;
    public T3 third;

    public Triple(T1 f, T2 s, T3 t) {         
        this.first  = f;
        this.second = s;
        this.third  = t;
    }
}