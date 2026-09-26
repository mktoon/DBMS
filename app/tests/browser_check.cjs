// Visual and browser interaction checks, run against the temporary smoke app.
// Playwright is CI/local test tooling only; the application needs no Node runtime.
const {chromium} = require(process.env.PLAYWRIGHT_MODULE_PATH || 'playwright');
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');

(async () => {
  const output = path.resolve(__dirname, '../target/browser');
  fs.mkdirSync(output, {recursive: true});
  const browser = await chromium.launch({executablePath: process.env.CHROME_PATH || undefined, args: ['--no-sandbox']});
  try {
    const page = await browser.newPage({viewport: {width: 1440, height: 1000}});
    const errors = [];
    page.on('pageerror', error => errors.push(error.message));
    await page.goto(process.env.SMOKE_BASE + '/login');
    await page.screenshot({path: path.join(output, 'login.png')});
    await page.getByLabel('Username', {exact: true}).fill('test_staff');
    await page.getByLabel('Password', {exact: true}).fill(process.env.SMOKE_PASSWORD);
    await Promise.all([page.waitForURL(process.env.SMOKE_BASE + '/'), page.getByRole('button', {name: 'Sign in'}).click()]);
    for (const [route, name] of [['/', 'overview'], ['/patients/new', 'registration'], ['/appointments', 'appointments'], ['/physicians', 'physicians'], ['/reports', 'reports']]) {
      const response = await page.goto(process.env.SMOKE_BASE + route);
      assert.equal(response.status(), 200, route);
      if (route === '/appointments' || route === '/reports') {
        for (const input of await page.locator('input[type="date"]').all()) {
          assert.match(await input.inputValue(), /^\d{4}-\d{2}-\d{2}$/, route + ' must prefill a usable date');
        }
      }
      assert.equal(await page.evaluate(() => document.documentElement.scrollWidth > innerWidth), false, route + ' overflows desktop viewport');
      await page.screenshot({path: path.join(output, name + '.png')});
    }
    await page.goto(process.env.SMOKE_BASE + '/patients/new');
    await page.getByLabel('Full name').fill('Browser Demo Patient');
    await page.getByLabel('Date of birth').fill('1992-05-11');
    await Promise.all([page.waitForURL(/\/patients\/\d+$/), page.getByRole('button', {name: 'Register patient'}).click()]);
    assert.equal(await page.getByRole('heading', {name: 'Browser Demo Patient', exact: true}).count(), 1);
    const patientUrl = page.url();
    await page.getByRole('link', {name: 'Edit details', exact: true}).click();
    assert.equal(await page.getByLabel('Date of birth').inputValue(), '1992-05-11');
    await page.goto(patientUrl);
    await page.getByRole('link', {name: '+ Book appointment', exact: true}).click();
    assert.equal(await page.getByRole('heading', {name: 'Book a visit', exact: true}).count(), 1);
    await page.screenshot({path: path.join(output, 'booking.png')});
    await page.setViewportSize({width: 390, height: 844});
    for (const [route, name] of [['/', 'overview-mobile'], ['/patients/new', 'registration-mobile'], ['/appointments', 'appointments-mobile']]) {
      await page.goto(process.env.SMOKE_BASE + route);
      await page.screenshot({path: path.join(output, name + '.png'), fullPage: true});
      const overflow = await page.evaluate(() => ({
        width: innerWidth, documentWidth: document.documentElement.scrollWidth,
        elements: [...document.querySelectorAll('body, .sidebar, .workspace, .topbar, main, .page-heading, .stats, .stat, .panel, .table-wrap, .quick-grid, .quick-card')]
          .map(element => ({element: element.className || element.tagName, left: element.getBoundingClientRect().left, right: element.getBoundingClientRect().right}))
          .filter(element => element.right > innerWidth || element.left < 0)
      }));
      assert.equal(overflow.documentWidth > overflow.width, false, route + ' overflows mobile viewport: ' + JSON.stringify(overflow));
    }
    assert.deepEqual(errors, []);
    console.log('Browser checks passed: sign-in, registration, booking navigation, and desktop/mobile layouts.');
  } finally {
    await browser.close();
  }
})().catch(error => {console.error(error);process.exitCode=1;});
