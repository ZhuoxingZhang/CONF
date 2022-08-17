select
    p_brand,
    p_type,
    p_size,
    count(distinct ps_suppkey) as supplier_cnt 
from
    partsupp,
    part
where
    p_partkey = ps_partkey
    and p_brand <> 'Brand#32'
    and p_type not like 'SMALL BRUSHED TIN%' 
and p_size in (1, 3, 5, 7, 9, 11, 13, 15)
    and ps_suppkey not in ( 
        select
            s_suppkey
        from
            supplier
        where
            s_comment like '%Customer%Complaints%'
    )
group by 
    p_brand,
    p_type,
    p_size
order by 
    supplier_cnt desc, 
    p_brand,
    p_type,
    p_size;