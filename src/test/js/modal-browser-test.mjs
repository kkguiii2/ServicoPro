import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { readFile } from 'node:fs/promises';
import http from 'node:http';
import path from 'node:path';

const projectRoot = path.resolve(import.meta.dirname, '..', '..', '..');
const layout = await readFile(
    path.join(projectRoot, 'src/main/resources/templates/layout/base.html'),
    'utf8'
);
const componentsCss = await readFile(
    path.join(projectRoot, 'src/main/resources/static/css/components.css'),
    'utf8'
);

const controllerStart = layout.indexOf('(function setupModals()');
const controllerEnd = layout.indexOf('})();', controllerStart);
assert.notEqual(controllerStart, -1, 'Controlador global de modal não encontrado');
assert.notEqual(controllerEnd, -1, 'Fim do controlador global de modal não encontrado');
const modalController = layout.slice(controllerStart, controllerEnd + 5);

const page = `<!doctype html>
<html lang="pt-BR">
<head>
    <meta charset="utf-8">
    <link id="componentsCss" rel="stylesheet" href="/components.css">
</head>
<body>
    <button id="open" data-modal-open="modalTeste" aria-expanded="false">Cadastrar</button>
    <div id="modalTeste" class="modal-backdrop" data-modal hidden aria-hidden="true">
        <div class="modal-panel" tabindex="-1">
            <button id="closeX" data-modal-close>X</button>
            <button id="cancel" data-modal-close>Cancelar</button>
            <input id="field">
        </div>
    </div>
    <pre id="result"></pre>
    <script>
        const listenerCounts = {};
        const nativeAddEventListener = document.addEventListener.bind(document);
        document.addEventListener = function(type, listener, options) {
            listenerCounts[type] = (listenerCounts[type] || 0) + 1;
            return nativeAddEventListener(type, listener, options);
        };
        ${modalController}
        ${modalController}

        const modal = document.getElementById('modalTeste');
        const trigger = document.getElementById('open');
        const states = [];
        const state = step => states.push({
            step,
            hidden: modal.hidden,
            open: modal.classList.contains('is-open'),
            ariaHidden: modal.getAttribute('aria-hidden'),
            expanded: trigger.getAttribute('aria-expanded'),
            display: getComputedStyle(modal).display
        });

        state('initial');
        trigger.click();
        state('opened');
        document.getElementById('closeX').click();
        state('closedByX');
        trigger.click();
        document.getElementById('cancel').click();
        state('closedByCancel');
        trigger.click();
        modal.click();
        state('closedByBackdrop');
        trigger.click();
        document.dispatchEvent(new KeyboardEvent('keydown', {
            key: 'Escape', bubbles: true, cancelable: true
        }));
        state('closedByEscape');

        // Mesmo sem a folha de estilos, hidden continua impedindo o aprisionamento.
        document.getElementById('componentsCss').disabled = true;
        trigger.click();
        state('openedWithoutCss');
        document.getElementById('cancel').click();
        state('closedWithoutCss');

        document.getElementById('result').textContent = JSON.stringify({
            listenerCounts,
            states
        });
    </script>
</body>
</html>`;

const server = http.createServer((request, response) => {
    if (request.url === '/components.css') {
        response.writeHead(200, { 'Content-Type': 'text/css; charset=utf-8' });
        response.end(componentsCss);
        return;
    }
    response.writeHead(200, { 'Content-Type': 'text/html; charset=utf-8' });
    response.end(page);
});

await new Promise((resolve, reject) => {
    server.once('error', reject);
    server.listen(0, '127.0.0.1', resolve);
});

try {
    const address = server.address();
    const edge = path.join(
        process.env['PROGRAMFILES(X86)'] || 'C:/Program Files (x86)',
        'Microsoft/Edge/Application/msedge.exe'
    );
    const html = await new Promise((resolve, reject) => {
        const browser = spawn(edge, [
            '--headless',
            '--disable-gpu',
            '--no-first-run',
            '--dump-dom',
            `http://127.0.0.1:${address.port}/`
        ]);
        let stdout = '';
        let stderr = '';
        browser.stdout.on('data', chunk => stdout += chunk);
        browser.stderr.on('data', chunk => stderr += chunk);
        browser.once('error', reject);
        browser.once('close', code => {
            if (code === 0) resolve(stdout);
            else reject(new Error(`Edge encerrou com código ${code}: ${stderr}`));
        });
    });

    const resultMatch = html.match(/<pre id="result">([\s\S]*?)<\/pre>/);
    assert.ok(resultMatch, 'Resultado do navegador não encontrado');
    const result = JSON.parse(resultMatch[1]
        .replaceAll('&quot;', '"')
        .replaceAll('&amp;', '&'));

    assert.deepEqual(result.listenerCounts, { click: 1, keydown: 1 });
    assert.deepEqual(result.states, [
        { step: 'initial', hidden: true, open: false, ariaHidden: 'true', expanded: 'false', display: 'none' },
        { step: 'opened', hidden: false, open: true, ariaHidden: 'false', expanded: 'true', display: 'grid' },
        { step: 'closedByX', hidden: true, open: false, ariaHidden: 'true', expanded: 'false', display: 'none' },
        { step: 'closedByCancel', hidden: true, open: false, ariaHidden: 'true', expanded: 'false', display: 'none' },
        { step: 'closedByBackdrop', hidden: true, open: false, ariaHidden: 'true', expanded: 'false', display: 'none' },
        { step: 'closedByEscape', hidden: true, open: false, ariaHidden: 'true', expanded: 'false', display: 'none' },
        { step: 'openedWithoutCss', hidden: false, open: true, ariaHidden: 'false', expanded: 'true', display: 'block' },
        { step: 'closedWithoutCss', hidden: true, open: false, ariaHidden: 'true', expanded: 'false', display: 'none' }
    ]);

    console.log('Chromium modal behavior: PASS');
} finally {
    await new Promise(resolve => server.close(resolve));
}
