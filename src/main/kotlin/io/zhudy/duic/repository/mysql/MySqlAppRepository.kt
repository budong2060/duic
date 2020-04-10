package io.zhudy.duic.repository.mysql

import io.zhudy.duic.domain.App
import io.zhudy.duic.repository.AbstractAppRepository
import io.zhudy.duic.vo.AppVo
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.data.r2dbc.core.DatabaseClient
import org.springframework.data.r2dbc.query.Criteria
import org.springframework.data.r2dbc.query.Criteria.from
import org.springframework.data.r2dbc.query.Criteria.where
import reactor.core.publisher.Mono

/**
 * @author Kevin Zou (kevinz@weghst.com)
 */
class MySqlAppRepository(
        private val dc: DatabaseClient
) : AbstractAppRepository(dc) {

    companion object {
        private const val NEXT_GV_SQL = "update duic_app_gv_seq set id=last_insert_id(id + 1); select last_insert_id();"
    }

    override fun nextGv(): Mono<Long> = Mono.defer {
        dc.execute(NEXT_GV_SQL)
                .map { row -> row.get(0, Long::class.java) }
                .one()
    }

    override fun search(vo: AppVo.UserQuery, pageable: Pageable): Mono<Page<App>> = Mono.defer {
        val filters = ArrayList<Criteria>(2)
        if (vo.q != null) {
            val s = "%${vo.q}%"
            filters.add(where("name").like(s).or("profile").like(s).or("description").like(s).or("content").like(s))
        }
        if (vo.email != null) {
            filters.add(where("email").like("%${vo.email}%"))
        }


        dc.select().from("DUIC_APP").matching(from(filters)).project("count(*)")

        val sql = StringBuilder("SELECT * FROM DUIC_APP WHERE 1=1")
        val countSql = StringBuilder("SELECT COUNT(*) FROM DUIC_APP WHERE 1=1")

        val params = mutableListOf<Pair<String, Any>>()
        val where = StringBuilder()
        if (vo.q != null) {
            where.append(" AND MATCH(name, profile, content) AGAINST(:q)")
            params.add("q" to vo.q)
        }
        if (vo.email != null) {
            where.append(" AND users LIKE CONCAT('%', :email, '%')")
            params.add("email" to vo.email)
        }
        sql.append(where).append(" LIMIT :offset,:limit")
        countSql.append(where)

        // query
        var spec = dc.execute(sql.toString())
                .bind("offset", pageable.offset)
                .bind("limit", pageable.pageSize)

        // count
        var specCount = dc.execute(countSql.toString())
        if (params.isNotEmpty()) {
            for (o in params) {
                spec = spec.bind(o.first, o.second)
                specCount = specCount.bind(o.first, o.second)
            }
        }

        spec.map(::mapToApp).all().collectList()
                .zipWith(
                        specCount.map { row -> row.get(0, Long::class.java) }.one()
                )
                .map {
                    PageImpl(it.t1, pageable, it.t2)
                }
    }
}