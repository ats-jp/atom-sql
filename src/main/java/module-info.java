/**
 * Atom Sql
 */
module jp.ats.atomsql {

	requires transitive java.sql;

	requires transitive java.compiler;

	exports jp.ats.atomsql;

	exports jp.ats.atomsql.annotation;

	exports jp.ats.atomsql.annotation.processor;

	exports jp.ats.atomsql.type;
}
