call p_split_address_multi_values();

select distinct val from(
	select distinct name as val from bas_region
	union
	select distinct village as val from addr_address
	union
	select distinct road as val from addr_address
	union
	select distinct val from tmp_splitted_values
)t order by char_length(val) desc
into outfile '/tmp/region.dic' 
fields terminated by ',' lines terminated by '\n';