call p_split_address_multi_values();

select distinct val from(
	select distinct name as val from bas_region
	union
	select distinct village as val from addr_address where village<>''
	union
	select distinct road as val from addr_address where road<>''
	union
	select distinct road_num as val from addr_address where road_num<>''
	union
	select distinct val from tmp_splitted_values where val<>''
)t order by char_length(val) desc
into outfile '/tmp/region.dic' 
lines terminated by '\n';