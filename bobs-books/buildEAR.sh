#!/bin/sh

CURRENT_DIR=$PWD

cd $CURRENT_DIR/bobbys-books/bobbys-front-end

mvn package

cd $CURRENT_DIR/bobs-bookstore-order-manager/

mvn package

cd $CURRENT_DIR

rm -rf eardir
rm bobs-book-store.ear

mkdir eardir

cd eardir

mkdir META-INF

cp $CURRENT_DIR/application.xml $CURRENT_DIR/eardir/META-INF/

cp $CURRENT_DIR/bobbys-books/bobbys-front-end/target/bobbys-front-end.war $CURRENT_DIR/eardir/

cp $CURRENT_DIR/bobs-bookstore-order-manager/target/bobs-bookstore-order-manager.war $CURRENT_DIR/eardir/

cd $CURRENT_DIR/eardir/

zip -r $CURRENT_DIR/bobs-book-store.ear .

cd $CURRENT_DIR

rm -rf eardir
