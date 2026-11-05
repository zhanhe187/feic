// webpack-dev-server 3.x 在 Node 18+ 下，WebSocket(/ws SockJS 聊天) 代理连接被重置时会抛出
// 未捕获的 socket error，直接把 dev-server 进程搞崩（表现为页面 chunk 全部 ERR_CONNECTION_REFUSED）。
// 这里兜底吞掉这类异常，保证前端开发服务器不会因为聊天 ws 断开而整体挂掉。
process.on('uncaughtException', (err) => {
    console.error('[dev-server] swallowed uncaught exception:', err && err.message);
});
process.on('unhandledRejection', (err) => {
    console.error('[dev-server] swallowed unhandled rejection:', err && err.message);
});

let proxyObj = {};
const CompressionPlugin = require("compression-webpack-plugin");
proxyObj['/ws'] = {
    ws: true,
    target: "ws://localhost:8081",
    onError: (err, req, res) => {
        console.error('[proxy:/ws]', err && err.message);
        if (res && res.end) { try { res.end(); } catch (e) { /* ignore */ } }
    }
};
proxyObj['/'] = {
    ws: false,
    target: 'http://localhost:8081',
    changeOrigin: true,
    pathRewrite: {
        '^/': ''
    },
    onError: (err, req, res) => {
        console.error('[proxy:/]', err && err.message);
        if (res && res.end) { try { res.end(); } catch (e) { /* ignore */ } }
    }
}
module.exports = {
    devServer: {
        host: 'localhost',
        // 默认 8084：8080 留给服务交付系统的后端，8082/8083 常被本机其他服务占用
        port: 8084,
        proxy: proxyObj
    },
    css: {
        loaderOptions: {
            // node-sass 4.x 无法在 Node 16+ 上编译，这里改用 dart-sass（sass 包）
            scss: {
                implementation: require('sass')
            }
        }
    },
    configureWebpack: config => {
        if (process.env.NODE_ENV === 'production') {
            return {
                plugins: [
                    new CompressionPlugin({
                        test: /\.js$|\.html$|\.css/,
                        threshold: 1024,
                        deleteOriginalAssets: false
                    })
                ]
            }
        }
    }
}
